package com.gigafix.forum.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Category;
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

		// 建立文章
		Article article = new Article();
		article.setAuthor(author);
		article.setCategory(category);
		article.setTitle(request.getTitle());
		article.setContent(request.getContent());
		article.setCoverImage(request.getCoverImage());

		// 發文即視為送出，依規劃書狀態機直接轉為已發布（@PrePersist 若未指定狀態預設是草稿 DRAFT，這裡明確覆蓋）
		article.setStatus(Article.ArticleStatus.PUBLISHED);

		// 儲存文章（viewCount/likeCount/commentCount/isPinned/建立時間交給 @PrePersist 處理）
		Article savedArticle = articleRepository.save(article);

		return toArticleResponse(savedArticle);
	}

	// 文章列表
	@Override
	public Page<ArticleResponse> getArticles(Integer categoryId, String keyword, String sort, int page, int size) {

		// 排序方式：popular = 依按讚數，其餘（含預設）= 依建立時間新到舊
		Sort sortOrder = "popular".equalsIgnoreCase(sort)
				? Sort.by(Sort.Direction.DESC, "likeCount")
				: Sort.by(Sort.Direction.DESC, "articleCreatedTime");

		Pageable pageable = PageRequest.of(page, size, sortOrder);

		// 只查已發布的文章，分類/關鍵字為選填條件
		Page<Article> articles = articleRepository.search(
				Article.ArticleStatus.PUBLISHED, categoryId, keyword, pageable);

		return articles.map(this::toArticleResponse);
	}

	// 文章詳情（瀏覽數 +1）
	@Override
	@Transactional
	public ArticleResponse getArticle(Long articleId) {

		// 查詢文章，非已發布狀態視為不存在
		Article article = articleRepository.findById(articleId)
				.filter(a -> a.getStatus() == Article.ArticleStatus.PUBLISHED)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 瀏覽數 +1
		article.setViewCount(article.getViewCount() + 1);
		Article savedArticle = articleRepository.save(article);

		return toArticleResponse(savedArticle);
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

	// ---------------後台審核功能（暫無權限檢查）----------------------

	// 審核／下架／置頂
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public ArticleResponse updateArticleStatus(Long articleId, UpdateArticleStatusRequest request) {

		// 查詢文章
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 更新狀態
		article.setStatus(request.getStatus());

		// 置頂狀態為選填，null 表示不變更
		if (request.getIsPinned() != null) {
			article.setIsPinned(request.getIsPinned());
		}

		Article savedArticle = articleRepository.save(article);

		return toArticleResponse(savedArticle);
	}

	// 將 Article Entity 轉成 ArticleResponse DTO
	private ArticleResponse toArticleResponse(Article article) {

		return ArticleResponse.builder()
				.articleId(article.getArticleId())
				.categoryId(article.getCategory().getCategoryId())
				.categoryName(article.getCategory().getName())
				.authorId(article.getAuthor().getId())
				.authorNickName(article.getAuthor().getNickName())
				.title(article.getTitle())
				.content(article.getContent())
				.viewCount(article.getViewCount())
				.likeCount(article.getLikeCount())
				.commentCount(article.getCommentCount())
				.coverImage(article.getCoverImage())
				.status(article.getStatus().name())
				.isPinned(article.getIsPinned())
				.articleCreatedTime(article.getArticleCreatedTime())
				.articleUpdatedTime(article.getArticleUpdatedTime())
				.build();
	}
}
