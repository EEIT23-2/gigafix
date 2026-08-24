package com.gigafix.forum.repository;

import java.util.Collection;
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

	// 前台文章列表查詢：狀態限定在可公開瀏覽的集合，且排除樓層（parent_article_id IS NULL），分類/關鍵字皆為選填
	@Query("SELECT a FROM Article a WHERE a.status IN :statuses AND a.parentArticle IS NULL "
			+ "AND (:categoryId IS NULL OR a.category.categoryId = :categoryId) "
			+ "AND (:keyword IS NULL OR a.title LIKE %:keyword% OR a.content LIKE %:keyword%)")
	Page<Article> search(@Param("statuses") Collection<Article.ArticleStatus> statuses,
			@Param("categoryId") Integer categoryId, @Param("keyword") String keyword, Pageable pageable);

	// 熱門排序：讚數 > 蓋樓數 > 瀏覽數，蓋樓數用 correlated subquery 現算（沒有落地欄位，避免要對既有資料 backfill），
	// 排序完全寫死在 JPQL 裡，呼叫端要傳「不帶 Sort」的 Pageable，避免跟這裡的 ORDER BY 衝突
	@Query("SELECT a FROM Article a WHERE a.status IN :statuses AND a.parentArticle IS NULL "
			+ "AND (:categoryId IS NULL OR a.category.categoryId = :categoryId) "
			+ "AND (:keyword IS NULL OR a.title LIKE %:keyword% OR a.content LIKE %:keyword%) "
			+ "ORDER BY a.isPinned DESC, a.likeCount DESC, "
			+ "(SELECT COUNT(f) FROM Article f WHERE f.parentArticle = a) DESC, "
			+ "a.viewCount DESC")
	Page<Article> searchOrderByPopularity(@Param("statuses") Collection<Article.ArticleStatus> statuses,
			@Param("categoryId") Integer categoryId, @Param("keyword") String keyword, Pageable pageable);

	// 後台文章列表查詢：狀態選填（null 代表不篩選），不排除樓層，多一個作者篩選
	@Query("SELECT a FROM Article a WHERE (:status IS NULL OR a.status = :status) "
			+ "AND (:categoryId IS NULL OR a.category.categoryId = :categoryId) "
			+ "AND (:authorId IS NULL OR a.author.id = :authorId) "
			+ "AND (:keyword IS NULL OR a.title LIKE %:keyword% OR a.content LIKE %:keyword%)")
	Page<Article> searchForAdmin(@Param("status") Article.ArticleStatus status,
			@Param("categoryId") Integer categoryId, @Param("authorId") Long authorId,
			@Param("keyword") String keyword, Pageable pageable);

	// 樓層查詢：依根文章 id 撈所有樓層，依建立時間正序（樓層編號在 Service 層依序計算，不落地存欄位）
	List<Article> findByParentArticle_ArticleIdOrderByArticleCreatedTimeAsc(Long parentArticleId);

	// 蓋樓時計算「目前已有幾樓」用
	long countByParentArticle_ArticleId(Long parentArticleId);
}
