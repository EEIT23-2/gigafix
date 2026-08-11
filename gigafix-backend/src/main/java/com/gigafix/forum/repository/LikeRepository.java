package com.gigafix.forum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Like;

public interface LikeRepository extends JpaRepository<Like, Long> {

	Optional<Like> findByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);

	Optional<Like> findByMember_IdAndComment_CommentId(Long memberId, Long commentId);

	long countByArticle_ArticleId(Long articleId);

	long countByComment_CommentId(Long commentId);

	void deleteByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);

	void deleteByMember_IdAndComment_CommentId(Long memberId, Long commentId);
}
