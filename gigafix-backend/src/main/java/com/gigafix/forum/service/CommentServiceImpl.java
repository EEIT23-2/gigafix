package com.gigafix.forum.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.CommentResponse;
import com.gigafix.forum.dto.CreateCommentRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Comment;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.CommentRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

	// 留言 Repository
	private final CommentRepository commentRepository;

	// 文章 Repository
	private final ArticleRepository articleRepository;

	// 會員 Repository
	private final MemberRepository memberRepository;

	// 留言
	@Override
	@Transactional
	public CommentResponse createComment(Long memberId, Long articleId, CreateCommentRequest request) {

		// 檢查會員是否存在
		Member author = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 檢查文章是否存在
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 只有已發布的文章可以留言
		if (article.getStatus() != Article.ArticleStatus.PUBLISHED) {
			throw new IllegalStateException("文章目前無法留言");
		}

		// 建立留言
		Comment comment = new Comment();
		comment.setArticle(article);
		comment.setAuthor(author);
		comment.setContent(request.getContent());

		// 儲存留言（likeCount/status/建立時間交給 @PrePersist 處理）
		Comment savedComment = commentRepository.save(comment);

		// 同步文章留言數 +1
		article.setCommentCount(article.getCommentCount() + 1);
		articleRepository.save(article);

		return toCommentResponse(savedComment);
	}

	// 查詢文章底下所有留言
	@Override
	public List<CommentResponse> getComments(Long articleId) {

		// 檢查文章是否存在
		if (!articleRepository.existsById(articleId)) {
			throw new IllegalArgumentException("文章不存在，articleId：" + articleId);
		}

		// 查詢文章底下所有留言，排除已下架的留言
		List<Comment> comments = commentRepository.findByArticle_ArticleIdOrderByCommentCreatedTimeAsc(articleId);

		return comments.stream()
				.filter(comment -> comment.getStatus() != Comment.CommentStatus.TAKEN_DOWN)
				.map(this::toCommentResponse)
				.toList();
	}

	// 刪除（軟刪除）自己的留言
	@Override
	@Transactional
	public void deleteComment(Long memberId, Long commentId) {

		// 檢查會員是否存在
		if (!memberRepository.existsById(memberId)) {
			throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
		}

		// 查詢留言
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new IllegalArgumentException("留言不存在，commentId：" + commentId));

		// 確認為留言作者本人
		if (!comment.getAuthor().getId().equals(memberId)) {
			throw new IllegalStateException("無權限操作此留言");
		}

		// 防止重複刪除
		if (comment.getStatus() == Comment.CommentStatus.TAKEN_DOWN) {
			throw new IllegalStateException("留言已下架");
		}

		// 軟刪除：改狀態為下架
		comment.setStatus(Comment.CommentStatus.TAKEN_DOWN);
		commentRepository.save(comment);

		// 同步文章留言數 -1
		Article article = comment.getArticle();
		article.setCommentCount(article.getCommentCount() - 1);
		articleRepository.save(article);
	}

	// 將 Comment Entity 轉成 CommentResponse DTO
	private CommentResponse toCommentResponse(Comment comment) {

		return CommentResponse.builder()
				.commentId(comment.getCommentId())
				.articleId(comment.getArticle().getArticleId())
				.authorId(comment.getAuthor().getId())
				.authorNickName(comment.getAuthor().getNickName())
				.content(comment.getContent())
				.likeCount(comment.getLikeCount())
				.status(comment.getStatus().name())
				.commentCreatedTime(comment.getCommentCreatedTime())
				.build();
	}
}
