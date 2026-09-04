package com.gigafix.forum.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gigafix.forum.entity.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	List<Bookmark> findByMember_IdOrderByBookmarkCreatedTimeDesc(Long memberId);

	Optional<Bookmark> findByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);

	// 一次查出某會員在指定文章集合中收藏過的 articleId，理由同 LikeRepository.findLikedArticleIds
	@Query("SELECT b.article.articleId FROM Bookmark b "
			+ "WHERE b.member.id = :memberId AND b.article.articleId IN :articleIds")
	List<Long> findBookmarkedArticleIds(@Param("memberId") Long memberId,
			@Param("articleIds") Collection<Long> articleIds);

	void deleteByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);
}
