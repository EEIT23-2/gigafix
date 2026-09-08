package com.gigafix.forum.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gigafix.forum.entity.Like;

public interface LikeRepository extends JpaRepository<Like, Long> {

	Optional<Like> findByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);

	// 一次查出某會員在指定文章集合中按過讚的 articleId。
	// 樓層列表要標示每一層是否已按讚，逐層查會變成 N 次查詢，改用這支一次撈完
	@Query("SELECT l.article.articleId FROM Like l "
			+ "WHERE l.member.id = :memberId AND l.article.articleId IN :articleIds")
	List<Long> findLikedArticleIds(@Param("memberId") Long memberId,
			@Param("articleIds") Collection<Long> articleIds);

	Optional<Like> findByMember_IdAndComment_CommentId(Long memberId, Long commentId);

	long countByArticle_ArticleId(Long articleId);

	long countByComment_CommentId(Long commentId);

	void deleteByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);

	void deleteByMember_IdAndComment_CommentId(Long memberId, Long commentId);
}
