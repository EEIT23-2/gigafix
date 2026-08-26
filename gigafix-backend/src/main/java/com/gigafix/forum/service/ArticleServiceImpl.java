package com.gigafix.forum.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.CreateFloorRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Category;
import com.gigafix.forum.exception.ForumException;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.CategoryRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

	// 文章 Repository
	private final ArticleRepository articleRepository;

	// 分類 Repository
	private final CategoryRepository categoryRepository;

	// 會員 Repository
	private final MemberRepository memberRepository;

	// 前台列表/詳情：完整可見的狀態（1=發布 4=關閉 6=強制關閉，關閉只鎖留言/蓋樓，文章本身仍完整可見）
	private static final Set<Article.ArticleStatus> FULLY_PUBLIC_STATUSES = EnumSet.of(
			Article.ArticleStatus.PUBLISHED, Article.ArticleStatus.CLOSED, Article.ArticleStatus.FORCE_CLOSED);

	// 會員可以自行轉換的目標狀態
	private static final Set<Article.ArticleStatus> MEMBER_ALLOWED_TARGETS = EnumSet.of(
			Article.ArticleStatus.PUBLISHED, Article.ArticleStatus.HIDDEN, Article.ArticleStatus.TAKEN_DOWN,
			Article.ArticleStatus.CLOSED);

	// 會員可以自行轉換的來源狀態（目前狀態必須落在這個集合內才允許變更）
	private static final Set<Article.ArticleStatus> MEMBER_ALLOWED_SOURCES = EnumSet.of(Article.ArticleStatus.DRAFT,
			Article.ArticleStatus.PUBLISHED, Article.ArticleStatus.HIDDEN, Article.ArticleStatus.CLOSED);

	// 後台可以轉換的目標狀態（管理員可操作：發布、下架、強制隱藏、強制關閉）
	private static final Set<Article.ArticleStatus> ADMIN_ALLOWED_TARGETS = EnumSet.of(Article.ArticleStatus.PUBLISHED,
			Article.ArticleStatus.TAKEN_DOWN, Article.ArticleStatus.FORCE_HIDDEN, Article.ArticleStatus.FORCE_CLOSED);

	// 後台可以變更狀態的來源（草稿、作者隱藏不開放管理員操作；強制處分後仍可再被管理員改回來，所以強制隱藏/強制關閉也是合法來源）
	private static final Set<Article.ArticleStatus> ADMIN_ALLOWED_SOURCES = EnumSet.of(Article.ArticleStatus.PUBLISHED,
			Article.ArticleStatus.CLOSED, Article.ArticleStatus.FORCE_HIDDEN, Article.ArticleStatus.FORCE_CLOSED);

	// ---------------會員前台功能----------------------

	// 發文
	@Override
	@Transactional
	public ArticleResponse createArticle(Long memberId, CreateArticleRequest request) {

		// 檢查會員是否存在
		Member author = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 檢查分類是否存在
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(
						() -> new IllegalArgumentException("分類不存在，categoryId：" + request.getCategoryId()));

		// 狀態選填，只接受草稿或發布；未填時預設發布，維持既有「發文即發布」行為
		Article.ArticleStatus requestedStatus = request.getStatus() != null ? request.getStatus()
				: Article.ArticleStatus.PUBLISHED;
		if (requestedStatus != Article.ArticleStatus.DRAFT && requestedStatus != Article.ArticleStatus.PUBLISHED) {
			throw ForumException.badRequest("建立文章時狀態僅能為草稿或發布");
		}
		// 只有要直接發布時才驗證標題/內文不能空白；草稿允許空白，供自動儲存流程使用
		if (requestedStatus == Article.ArticleStatus.PUBLISHED
				&& (isBlank(request.getTitle()) || isBlank(request.getContent()))) {
			throw ForumException.badRequest("標題與內文不能為空");
		}

		// 建立文章
		Article article = new Article();
		article.setAuthor(author);
		article.setCategory(category);
		article.setTitle(emptyIfNull(request.getTitle()));
		article.setContent(emptyIfNull(request.getContent()));
		article.setCoverImage(request.getCoverImage());
		article.setStatus(requestedStatus);

		// 儲存文章（viewCount/likeCount/commentCount/isPinned/建立時間交給 @PrePersist 處理）
		Article savedArticle = articleRepository.save(article);

		return toArticleResponse(savedArticle);
	}

	// 文章列表
	@Override
	public Page<ArticleResponse> getArticles(Integer categoryId, String keyword, String sort, int page, int size) {

		Page<Article> articles;
		if ("popular".equalsIgnoreCase(sort)) {
			// 熱門排序：讚 > 蓋樓 > 瀏覽，完整排序寫死在 JPQL 裡，Pageable 不能再帶 Sort，避免衝突
			Pageable pageable = PageRequest.of(page, size);
			articles = articleRepository.searchOrderByPopularity(FULLY_PUBLIC_STATUSES, categoryId, keyword, pageable);
		} else {
			// 預設（latest）：依建立時間新到舊；置頂文章一律排最前面
			Sort sortOrder = Sort.by(Sort.Direction.DESC, "isPinned")
					.and(Sort.by(Sort.Direction.DESC, "articleCreatedTime"));
			Pageable pageable = PageRequest.of(page, size, sortOrder);
			articles = articleRepository.search(FULLY_PUBLIC_STATUSES, categoryId, keyword, pageable);
		}

		return articles.map(this::toArticleResponse);
	}

	// 文章詳情（瀏覽數 +1；依狀態決定回傳完整內容或遮蔽版）
	@Override
	@Transactional
	public ArticleResponse getArticle(Long articleId, Long memberId) {

		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		ArticleResponse response = resolveVisibility(article, memberId, true);
		if (response == null) {
			// DRAFT 且非作者本人：視為不存在
			throw new IllegalArgumentException("文章不存在，articleId：" + articleId);
		}
		return response;
	}

	// 依文章狀態與呼叫者身份，決定回傳完整內容還是遮蔽版；回傳 null 代表呼叫者完全看不到（目前只有 DRAFT 給非作者會這樣）
	private ArticleResponse resolveVisibility(Article article, Long memberId, boolean incrementViewIfFull) {

		boolean isAuthor = memberId != null && article.getAuthor().getId().equals(memberId);
		Article.ArticleStatus status = article.getStatus();

		if (FULLY_PUBLIC_STATUSES.contains(status)) {
			if (incrementViewIfFull) {
				article.setViewCount(article.getViewCount() + 1);
				articleRepository.save(article);
			}
			return toArticleResponse(article, true, null);
		}
		if (status == Article.ArticleStatus.HIDDEN) {
			return isAuthor ? toArticleResponse(article, true, null)
					: toArticleResponse(article, false, "此文章目前已被作者隱藏");
		}
		if (status == Article.ArticleStatus.FORCE_HIDDEN) {
			return isAuthor ? toArticleResponse(article, true, null)
					: toArticleResponse(article, false, "此文章已被管理員隱藏");
		}
		if (status == Article.ArticleStatus.TAKEN_DOWN) {
			// 連作者本人透過這個公開端點都看不到完整內容，完整內容只有後台端點看得到
			return toArticleResponse(article, false, "此文章已下架");
		}
		// DRAFT：只有作者本人看得到，其他人視為不存在
		return isAuthor ? toArticleResponse(article, true, null) : null;
	}

	// 編輯自己的文章
	@Override
	@Transactional
	public ArticleResponse updateArticle(Long memberId, Long articleId, UpdateArticleRequest request) {

		// 檢查會員是否存在
		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		// 查詢文章
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 確認為文章作者本人
		if (!article.getAuthor().getId().equals(memberId)) {
			throw new IllegalStateException("無權限操作此文章");
		}

		// 檢查分類是否存在
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(
						() -> new IllegalArgumentException("分類不存在，categoryId：" + request.getCategoryId()));

		// 草稿階段允許標題/內文空白（自動儲存用）；非草稿狀態才驗證不能為空
		if (article.getStatus() != Article.ArticleStatus.DRAFT
				&& (isBlank(request.getTitle()) || isBlank(request.getContent()))) {
			throw ForumException.badRequest("標題與內文不能為空");
		}

		// 更新文章內容
		article.setCategory(category);
		article.setTitle(request.getTitle());
		article.setContent(request.getContent());
		article.setCoverImage(request.getCoverImage());

		Article savedArticle = articleRepository.save(article);

		return toArticleResponse(savedArticle);
	}

	// 刪除（軟刪除）自己的文章
	@Override
	@Transactional
	public void deleteArticle(Long memberId, Long articleId) {

		// 檢查會員是否存在
		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		// 查詢文章
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 確認為文章作者本人
		if (!article.getAuthor().getId().equals(memberId)) {
			throw new IllegalStateException("無權限操作此文章");
		}

		// 軟刪除：改狀態為下架
		article.setStatus(Article.ArticleStatus.TAKEN_DOWN);
		articleRepository.save(article);
	}

	// 會員自行變更自己文章的狀態
	@Override
	@Transactional
	public ArticleResponse updateOwnArticleStatus(Long memberId, Long articleId, UpdateArticleStatusRequest request) {

		// 檢查會員是否存在
		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		// 查詢文章
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 確認為文章作者本人
		if (!article.getAuthor().getId().equals(memberId)) {
			throw new IllegalStateException("無權限操作此文章");
		}

		// 目標狀態必須是會員自己可以設定的狀態
		if (!MEMBER_ALLOWED_TARGETS.contains(request.getStatus())) {
			throw new IllegalStateException("不允許轉換為此狀態：" + request.getStatus());
		}
		// 目前狀態必須允許會員自行變更（強制隱藏/強制關閉/下架後就不能再自行變更）
		if (!MEMBER_ALLOWED_SOURCES.contains(article.getStatus())) {
			throw new IllegalStateException("目前狀態不允許變更：" + article.getStatus());
		}
		// 發布時要驗證標題/內文不能為空
		if (request.getStatus() == Article.ArticleStatus.PUBLISHED
				&& (isBlank(article.getTitle()) || isBlank(article.getContent()))) {
			throw ForumException.badRequest("標題與內文不能為空，無法發布");
		}

		article.setStatus(request.getStatus());
		// isPinned 在此端點刻意忽略，置頂改由專門的後台 pin 端點處理

		return toArticleResponse(articleRepository.save(article));
	}

	// 會員自己的文章列表（個人中心用）
	@Override
	public List<ArticleResponse> getMyArticles(Long memberId) {

		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		return articleRepository.findByAuthor_IdOrderByArticleCreatedTimeDesc(memberId).stream()
				.filter(a -> a.getStatus() != Article.ArticleStatus.TAKEN_DOWN)
				.map(this::toArticleResponse)
				.toList();
	}

	// 樓層列表
	@Override
	public List<ArticleResponse> getFloors(Long articleId, Long memberId) {

		if (!articleRepository.existsById(articleId)) {
			throw new IllegalArgumentException("文章不存在，articleId：" + articleId);
		}

		List<Article> floors = articleRepository.findByParentArticle_ArticleIdOrderByArticleCreatedTimeAsc(articleId);

		List<ArticleResponse> result = new ArrayList<>();
		for (int i = 0; i < floors.size(); i++) {
			ArticleResponse response = resolveVisibility(floors.get(i), memberId, false);
			if (response != null) {
				response.setFloorNumber(i + 2); // 根文章本身視為 1 樓，第一則回覆是 2 樓
				result.add(response);
			}
		}
		return result;
	}

	// 蓋樓
	@Override
	@Transactional
	public ArticleResponse createFloor(Long memberId, Long articleId, CreateFloorRequest request) {

		Member author = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		Article root = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 防止樓中樓：只有頂層文章能被蓋樓
		if (root.getParentArticle() != null) {
			throw new IllegalStateException("不可在樓層下再建立樓層");
		}
		// 只有發布中的文章可以蓋樓（草稿/作者隱藏/下架/關閉/強制隱藏/強制關閉都不行）
		if (root.getStatus() != Article.ArticleStatus.PUBLISHED) {
			throw new IllegalStateException("文章目前無法蓋樓");
		}

		// 樓層編號：根文章視為 1 樓，目前已有幾樓 +2 就是新樓層的樓層數
		int floorNumber = (int) articleRepository.countByParentArticle_ArticleId(articleId) + 2;

		Article floor = new Article();
		floor.setAuthor(author);
		floor.setParentArticle(root);
		floor.setCategory(root.getCategory()); // 樓層繼承根文章分類
		floor.setTitle(root.getTitle() + "(" + floorNumber + "樓)");
		floor.setContent(request.getContent());
		floor.setStatus(Article.ArticleStatus.PUBLISHED); // 樓層不走草稿，直接發布

		Article saved = articleRepository.save(floor);

		ArticleResponse response = toArticleResponse(saved);
		response.setFloorNumber(floorNumber);
		return response;
	}

	// ---------------後台管理功能（暫無權限檢查）----------------------

	// 後台文章列表，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	public Page<ArticleResponse> getArticlesForAdmin(Article.ArticleStatus status, Integer categoryId,
			String keyword, Long authorId, int page, int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "articleCreatedTime"));

		return articleRepository.searchForAdmin(status, categoryId, authorId, keyword, pageable)
				.map(this::toArticleResponse);
	}

	// 後台文章詳情，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	public ArticleResponse getArticleForAdmin(Long articleId) {

		return articleRepository.findById(articleId)
				.map(this::toArticleResponse)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));
	}

	// 審核／下架／強制處分
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public ArticleResponse updateArticleStatus(Long articleId, UpdateArticleStatusRequest request) {

		// 查詢文章
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 目前狀態必須允許管理員操作（草稿、作者隱藏不開放）
		if (!ADMIN_ALLOWED_SOURCES.contains(article.getStatus())) {
			throw new IllegalStateException("目前狀態不允許管理員變更：" + article.getStatus());
		}
		// 後台可以轉換到發布/下架/強制隱藏/強制關閉（含解除處分改回發布）
		if (!ADMIN_ALLOWED_TARGETS.contains(request.getStatus())) {
			throw new IllegalStateException("不允許轉換為此狀態：" + request.getStatus());
		}
		article.setStatus(request.getStatus());

		// 置頂狀態為選填，null 表示不變更
		if (request.getIsPinned() != null) {
			article.setIsPinned(request.getIsPinned());
		}

		Article savedArticle = articleRepository.save(article);

		return toArticleResponse(savedArticle);
	}

	// 置頂／取消置頂
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public ArticleResponse updateArticlePin(Long articleId, Boolean isPinned) {

		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		article.setIsPinned(isPinned);

		return toArticleResponse(articleRepository.save(article));
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	// title/content 欄位為 NOT NULL，null 一律轉空字串再存進 entity，避免草稿完全不帶欄位時撞 DB 限制
	private static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}

	// 將 Article Entity 轉成 ArticleResponse DTO（完整內容）
	private ArticleResponse toArticleResponse(Article article) {
		return toArticleResponse(article, true, null);
	}

	// 將 Article Entity 轉成 ArticleResponse DTO；visible=false 時 title/content/coverImage 遮蔽
	private ArticleResponse toArticleResponse(Article article, boolean visible, String visibilityMessage) {

		return ArticleResponse.builder()
				.articleId(article.getArticleId())
				.categoryId(article.getCategory().getCategoryId())
				.categoryName(article.getCategory().getName())
				.authorId(article.getAuthor().getId())
				.authorNickName(article.getAuthor().getNickName())
				.title(visible ? article.getTitle() : null)
				.content(visible ? article.getContent() : null)
				.viewCount(article.getViewCount())
				.likeCount(article.getLikeCount())
				.commentCount(article.getCommentCount())
				.floorCount((int) articleRepository.countByParentArticle_ArticleId(article.getArticleId()))
				.coverImage(visible ? article.getCoverImage() : null)
				.status(article.getStatus().name())
				.isPinned(article.getIsPinned())
				.articleCreatedTime(article.getArticleCreatedTime())
				.articleUpdatedTime(article.getArticleUpdatedTime())
				.parentArticleId(article.getParentArticle() != null ? article.getParentArticle().getArticleId() : null)
				.visible(visible)
				.visibilityMessage(visibilityMessage)
				.build();
	}
}
