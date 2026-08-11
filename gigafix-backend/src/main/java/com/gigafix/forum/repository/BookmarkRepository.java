package com.gigafix.forum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	List<Bookmark> findByMember_IdOrderByBookmarkCreatedTimeDesc(Long memberId);

	Optional<Bookmark> findByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);

	void deleteByMember_IdAndArticle_ArticleId(Long memberId, Long articleId);
}
