package com.gigafix.forum.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.LikeResponse;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Comment;
import com.gigafix.forum.entity.Like;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.CommentRepository;
import com.gigafix.forum.repository.LikeRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

	// 讚 Repository
	private final LikeRepository likeRepository;

	// 文章 Repository
	private final ArticleRepository articleRepository;

	// 留言 Repository
	private final CommentRepository commentRepository;

	// 會員 Repository
	private final MemberRepository memberRepository;

	// 完整可見的狀態（1=發布 4=關閉 6=強制關閉），只有這些狀態的文章可以被按讚
	private static final Set<Article.ArticleStatus> FULLY_PUBLIC_STATUSES = EnumSet.of(
			Article.ArticleStatus.PUBLISHED, Article.ArticleStatus.CLOSED, Article.ArticleStatus.FORCE_CLOSED);

	// 對文章按讚
	@Override
	@Transactional
	public LikeResponse likeArticle(Long memberId, Long articleId) {

		// 檢查會員是否存在
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 查詢文章，非完整可見狀態視為不存在
		Article article = articleRepository.findById(articleId)
				.filter(a -> FULLY_PUBLIC_STATUSES.contains(a.getStatus()))
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 檢查是否已經讚過
		if (likeRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId).isPresent()) {
			throw new IllegalStateException("已經按過讚了");
		}

		// 建立讚
		Like like = new Like();
		like.setMember(member);
		like.setArticle(article);

		Like savedLike = likeRepository.save(like);

		// 同步文章按讚數 +1
		article.setLikeCount(article.getLikeCount() + 1);
		articleRepository.save(article);

		return LikeResponse.builder()
				.likeId(savedLike.getLikeId())
				.likeCount(article.getLikeCount())
				.build();
	}

	// 取消對文章的讚
	@Override
	@Transactional
	public void unlikeArticle(Long memberId, Long articleId) {

		// 查詢讚
		Like like = likeRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId)
				.orElseThrow(() -> new IllegalArgumentException("尚未按讚"));

		likeRepository.delete(like);

		// 同步文章按讚數 -1
		Article article = like.getArticle();
		article.setLikeCount(article.getLikeCount() - 1);
		articleRepository.save(article);
	}

	// 對留言按讚
	@Override
	@Transactional
	public LikeResponse likeComment(Long memberId, Long commentId) {

		// 檢查會員是否存在
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 查詢留言，已下架的留言視為不存在
		Comment comment = commentRepository.findById(commentId)
				.filter(c -> c.getStatus() != Comment.CommentStatus.TAKEN_DOWN)
				.orElseThrow(() -> new IllegalArgumentException("留言不存在，commentId：" + commentId));

		// 檢查是否已經讚過
		if (likeRepository.findByMember_IdAndComment_CommentId(memberId, commentId).isPresent()) {
			throw new IllegalStateException("已經按過讚了");
		}

		// 建立讚
		Like like = new Like();
		like.setMember(member);
		like.setComment(comment);

		Like savedLike = likeRepository.save(like);

		// 同步留言按讚數 +1
		comment.setLikeCount(comment.getLikeCount() + 1);
		commentRepository.save(comment);

		return LikeResponse.builder()
				.likeId(savedLike.getLikeId())
				.likeCount(comment.getLikeCount())
				.build();
	}

	// 取消對留言的讚
	@Override
	@Transactional
	public void unlikeComment(Long memberId, Long commentId) {

		// 查詢讚
		Like like = likeRepository.findByMember_IdAndComment_CommentId(memberId, commentId)
				.orElseThrow(() -> new IllegalArgumentException("尚未按讚"));

		likeRepository.delete(like);

		// 同步留言按讚數 -1
		Comment comment = like.getComment();
		comment.setLikeCount(comment.getLikeCount() - 1);
		commentRepository.save(comment);
	}

	// 查詢會員是否已對某篇文章按讚
	@Override
	public boolean hasLikedArticle(Long memberId, Long articleId) {
		return likeRepository.findByMember_IdAndArticle_ArticleId(memberId, articleId).isPresent();
	}

	// 查詢會員是否已對某則留言按讚
	@Override
	public boolean hasLikedComment(Long memberId, Long commentId) {
		return likeRepository.findByMember_IdAndComment_CommentId(memberId, commentId).isPresent();
	}
}
