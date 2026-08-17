package com.gigafix.forum.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gigafix.forum.entity.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {

	Page<Article> findByStatus(Article.ArticleStatus status, Pageable pageable);

	Page<Article> findByCategory_CategoryIdAndStatus(Integer categoryId, Article.ArticleStatus status,
			Pageable pageable);

	List<Article> findByAuthor_IdOrderByArticleCreatedTimeDesc(Long authorId);

	// 該分類是否還有文章在使用（刪除分類前檢查用）
	boolean existsByCategory_CategoryId(Integer categoryId);

	// 文章列表查詢：分類、關鍵字皆為選填（傳 null 代表不篩選），狀態固定帶入（給前台只查 PUBLISHED 用）
	@Query("SELECT a FROM Article a WHERE a.status = :status "
			+ "AND (:categoryId IS NULL OR a.category.categoryId = :categoryId) "
			+ "AND (:keyword IS NULL OR a.title LIKE %:keyword% OR a.content LIKE %:keyword%)")
	Page<Article> search(@Param("status") Article.ArticleStatus status, @Param("categoryId") Integer categoryId,
			@Param("keyword") String keyword, Pageable pageable);
}
