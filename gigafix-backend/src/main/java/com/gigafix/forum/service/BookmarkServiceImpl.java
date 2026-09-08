package com.gigafix.forum.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.BookmarkResponse;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Bookmark;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.BookmarkRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

	// 收藏 Repository
	private final BookmarkRepository bookmarkRepository;

	// 文章 Repository
	private final ArticleRepository articleRepository;

	// 會員 Repository
	private final MemberRepository memberRepository;

	// 完整可見的狀態（1=發布 4=關閉 6=強制關閉），只有這些狀態的文章可以被收藏
	private static final Set<Article.ArticleStatus> FULLY_PUBLIC_STATUSES = EnumSet.of(
			Article.ArticleStatus.PUBLISHED, Article.ArticleStatus.CLOSED, Article.ArticleStatus.FORCE_CLOSED);

	// 收藏文章
	@Override
	@Transactional
	public BookmarkResponse addBookmark(Long memberId, Long articleId) {

		// 檢查會員是否存在
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 查詢文章，非完整可見狀態視為不存在
		Article article = articleRepository.findById(articleId)
				.filter(a -> FULLY_PUBLIC_STATUSES.contains(a.getStatus()))
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 檢查是否已經收藏過
		if (bookmarkRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId).isPresent()) {
			throw new IllegalStateException("已經收藏過了");
		}

		// 建立收藏
		Bookmark bookmark = new Bookmark();
		bookmark.setMember(member);
		bookmark.setArticle(article);

		Bookmark savedBookmark = bookmarkRepository.save(bookmark);

		return toBookmarkResponse(savedBookmark);
	}

	// 取消收藏
	@Override
	@Transactional
	public void removeBookmark(Long memberId, Long articleId) {

		// 查詢收藏
		Bookmark bookmark = bookmarkRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId)
				.orElseThrow(() -> new IllegalArgumentException("尚未收藏此文章"));

		bookmarkRepository.delete(bookmark);
	}

	// 查詢自己收藏的文章列表
	@Override
	public List<BookmarkResponse> getBookmarks(Long memberId) {

		// 檢查會員是否存在
		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		List<Bookmark> bookmarks = bookmarkRepository.findByMember_IdOrderByBookmarkCreatedTimeDesc(memberId);

		return bookmarks.stream()
				.map(this::toBookmarkResponse)
				.toList();
	}

	// 查詢會員是否已收藏某篇文章
	@Override
	public boolean hasBookmarked(Long memberId, Long articleId) {
		return bookmarkRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId).isPresent();
	}

	// 將 Bookmark Entity 轉成 BookmarkResponse DTO
	private BookmarkResponse toBookmarkResponse(Bookmark bookmark) {

		return BookmarkResponse.builder()
				.bookmarkId(bookmark.getBookmarkId())
				.bookmarkCreatedTime(bookmark.getBookmarkCreatedTime())
				.article(toArticleResponse(bookmark.getArticle()))
				.build();
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
				.parentArticleId(article.getParentArticle() != null ? article.getParentArticle().getArticleId() : null)
				.articleCreatedTime(article.getArticleCreatedTime())
				.articleUpdatedTime(article.getArticleUpdatedTime())
				.articleEditedTime(article.getArticleEditedTime())
				// 能被收藏的文章一定通過了上面的 FULLY_PUBLIC_STATUSES 過濾，這裡固定完整可見
				.visible(true)
				.visibilityMessage(null)
				.build();
	}
}
