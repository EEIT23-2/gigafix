package com.gigafix.forum.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {

	Page<Article> findByStatus(Article.ArticleStatus status, Pageable pageable);

	Page<Article> findByCategory_CategoryIdAndStatus(Integer categoryId, Article.ArticleStatus status,
			Pageable pageable);

	List<Article> findByAuthor_IdOrderByArticleCreatedTimeDesc(Long authorId);
}
