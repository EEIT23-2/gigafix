package com.gigafix.forum.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
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
import com.gigafix.forum.dto.UpdateFloorRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Category;
import com.gigafix.forum.exception.ForumException;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.BookmarkRepository;
import com.gigafix.forum.repository.CategoryRepository;
import com.gigafix.forum.repository.LikeRepository;
import com.gigafix.forum.util.HtmlSanitizer;
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

	// 讚 Repository（詳情/樓層列表要附上呼叫者是否已按讚）
	private final LikeRepository likeRepository;

	// 收藏 Repository（詳情/樓層列表要附上呼叫者是否已收藏）
	private final BookmarkRepository bookmarkRepository;

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
		// 內文是富文本 HTML，空編輯器會送來 <p></p>，isBlank 擋不住，要用 isEmptyContent
		if (requestedStatus == Article.ArticleStatus.PUBLISHED
				&& (isBlank(request.getTitle()) || HtmlSanitizer.isEmptyContent(request.getContent()))) {
			throw ForumException.badRequest("標題與內文不能為空");
		}

		// 建立文章
		Article article = new Article();
		article.setAuthor(author);
		article.setCategory(category);
		article.setTitle(emptyIfNull(request.getTitle()));
		article.setContent(HtmlSanitizer.clean(emptyIfNull(request.getContent())));
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
		applySingleMemberInteraction(response, memberId);
		return response;
	}

	// 依文章狀態與呼叫者身份，決定回傳完整內容還是遮蔽版；回傳 null 代表呼叫者完全看不到（目前只有 DRAFT 給非作者會這樣）
	private ArticleResponse resolveVisibility(Article article, Long memberId, boolean incrementViewIfFull) {

		boolean isAuthor = memberId != null && article.getAuthor().getId().equals(memberId);
		Article.ArticleStatus status = article.getStatus();

		ArticleResponse response;
		if (FULLY_PUBLIC_STATUSES.contains(status)) {
			// 先把 DTO 組完（順便把 lazy 關聯讀出來），再去加瀏覽數，
			// 否則 incrementViewCount 清掉 persistence context 後這裡會抓不到 category/author
			response = toArticleResponse(article, true, null);
			if (incrementViewIfFull) {
				articleRepository.incrementViewCount(article.getArticleId());
				// entity 本身刻意不動（動了 dirty checking 一樣會觸發 @PreUpdate），所以自己把這次瀏覽補到回傳值上
				response.setViewCount(response.getViewCount() + 1);
			}
		} else if (status == Article.ArticleStatus.HIDDEN) {
			response = isAuthor ? toArticleResponse(article, true, null)
					: toArticleResponse(article, false, "此文章目前已被作者隱藏");
		} else if (status == Article.ArticleStatus.FORCE_HIDDEN) {
			response = isAuthor ? toArticleResponse(article, true, null)
					: toArticleResponse(article, false, "此文章已被管理員隱藏");
		} else if (status == Article.ArticleStatus.TAKEN_DOWN) {
			// 連作者本人透過這個公開端點都看不到完整內容，完整內容只有後台端點看得到
			response = toArticleResponse(article, false, "此文章已下架");
		} else if (isAuthor) {
			// DRAFT：只有作者本人看得到
			response = toArticleResponse(article, true, null);
		} else {
			// DRAFT 給非作者：視為不存在
			return null;
		}

		return response;
	}

	// 附上呼叫者對「單篇」文章的讚／收藏狀態（文章詳情用）。
	// 內容看不到時（已下架/被隱藏的遮蔽版）直接跳過，不必為了讀不到的內容多打兩次查詢
	private void applySingleMemberInteraction(ArticleResponse response, Long memberId) {

		if (memberId == null || !Boolean.TRUE.equals(response.getVisible())) {
			return;
		}
		Long articleId = response.getArticleId();
		response.setLikedByCurrentMember(
				likeRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId).isPresent());
		response.setBookmarkedByCurrentMember(
				bookmarkRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId).isPresent());
	}

	// 附上呼叫者對「一批」文章的讚／收藏狀態（樓層列表用）。
	// 逐層查會變成每層 2 次查詢，這裡改成整批各查一次，總共固定 2 次
	private void applyBatchMemberInteraction(List<ArticleResponse> responses, Long memberId) {

		// 只有看得到內容的才需要標示互動狀態
		List<Long> targetIds = responses.stream()
				.filter(r -> Boolean.TRUE.equals(r.getVisible()))
				.map(ArticleResponse::getArticleId)
				.toList();

		if (memberId == null || targetIds.isEmpty()) {
			return;
		}

		Set<Long> likedIds = new HashSet<>(likeRepository.findLikedArticleIds(memberId, targetIds));
		Set<Long> bookmarkedIds = new HashSet<>(bookmarkRepository.findBookmarkedArticleIds(memberId, targetIds));

		for (ArticleResponse response : responses) {
			if (Boolean.TRUE.equals(response.getVisible())) {
				response.setLikedByCurrentMember(likedIds.contains(response.getArticleId()));
				response.setBookmarkedByCurrentMember(bookmarkedIds.contains(response.getArticleId()));
			}
		}
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

		// 樓層擋在這裡：樓層也是一筆 article，不擋的話這支端點會變成改樓層標題與分類的後門
		// （兩者都是後端依樓主推導出來的，不該由呼叫者決定）
		if (article.getParentArticle() != null) {
			throw ForumException.badRequest("樓層請改用樓層編輯端點");
		}

		// 檢查分類是否存在
		Category category = categoryRepository.findById(request.getCategoryId())
				.orElseThrow(
						() -> new IllegalArgumentException("分類不存在，categoryId：" + request.getCategoryId()));

		// 草稿階段允許標題/內文空白（自動儲存用）；非草稿狀態才驗證不能為空
		if (article.getStatus() != Article.ArticleStatus.DRAFT
				&& (isBlank(request.getTitle()) || HtmlSanitizer.isEmptyContent(request.getContent()))) {
			throw ForumException.badRequest("標題與內文不能為空");
		}

		// 更新文章內容
		article.setCategory(category);
		article.setTitle(request.getTitle());
		article.setContent(HtmlSanitizer.clean(request.getContent()));
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
				&& (isBlank(article.getTitle()) || HtmlSanitizer.isEmptyContent(article.getContent()))) {
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
		// 整批一次帶回讚／收藏狀態，避免逐層查詢
		applyBatchMemberInteraction(result, memberId);
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

		// 樓層內容也是富文本，空編輯器輸出是 <p></p>，@NotBlank 擋不住
		if (HtmlSanitizer.isEmptyContent(request.getContent())) {
			throw ForumException.badRequest("樓層內容不能為空");
		}

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
		floor.setContent(HtmlSanitizer.clean(request.getContent()));
		floor.setStatus(Article.ArticleStatus.PUBLISHED); // 樓層不走草稿，直接發布

		Article saved = articleRepository.save(floor);

		ArticleResponse response = toArticleResponse(saved);
		response.setFloorNumber(floorNumber);
		return response;
	}

	// 編輯樓層：只換內文。標題（樓主標題+樓層數）與分類是後端產生的，這支端點刻意不接受它們，
	// 前端也就不需要（也不可能）把伺服器自己算出來的值再送回來
	@Override
	@Transactional
	public ArticleResponse updateFloor(Long memberId, Long floorId, UpdateFloorRequest request) {

		// 檢查會員是否存在
		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		Article floor = articleRepository.findById(floorId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + floorId));

		// 這支端點只處理樓層；一般文章請走 updateArticle（那邊才有標題與分類）
		Article root = floor.getParentArticle();
		if (root == null) {
			throw ForumException.badRequest("此文章不是樓層");
		}

		// 確認為樓層作者本人
		if (!floor.getAuthor().getId().equals(memberId)) {
			throw new IllegalStateException("無權限操作此文章");
		}

		// 樓層自己的狀態必須是發布中。
		// 不能只靠前端的 visible 判斷：resolveVisibility 對 FORCE_HIDDEN 的樓層，
		// 作者本人拿到的 visible 仍是 true，等於「被管理員隱藏後還能自行改稿」
		if (floor.getStatus() != Article.ArticleStatus.PUBLISHED) {
			throw new IllegalStateException("此樓層目前無法編輯");
		}

		// 樓主文章也必須是發布中，與 createFloor 同一道守衛。
		// 這條同時涵蓋了關閉（CLOSED/FORCE_CLOSED）：討論串凍結後既有內容也不該再被改動
		if (root.getStatus() != Article.ArticleStatus.PUBLISHED) {
			throw new IllegalStateException("文章目前無法編輯樓層");
		}

		// 空編輯器輸出是 <p></p>，@NotBlank 擋不住
		if (HtmlSanitizer.isEmptyContent(request.getContent())) {
			throw ForumException.badRequest("樓層內容不能為空");
		}

		// articleUpdatedTime 由 Article 的 @PreUpdate 自動帶上，不用手動設
		floor.setContent(HtmlSanitizer.clean(request.getContent()));

		return toArticleResponse(articleRepository.save(floor));
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
				// 樓層底下不會再有樓層（樓中樓在建立時就被擋掉），所以樓層的 floorCount 結構上恆為 0，
				// 不需要為每一層都查一次資料庫
				.floorCount(article.getParentArticle() != null ? 0
						: (int) articleRepository.countByParentArticle_ArticleId(article.getArticleId()))
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
