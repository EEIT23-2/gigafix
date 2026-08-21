package com.gigafix.forum.service;

import org.springframework.data.domain.Page;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;

/**
 * 文章 Service
 * 定義文章相關商業功能
 */
public interface ArticleService {

	// ---------------會員前台功能------------------

	// 發文
	ArticleResponse createArticle(Long memberId, CreateArticleRequest request);

	// 文章列表（分類篩選、關鍵字搜尋、排序、分頁），只回傳已發布的文章
	Page<ArticleResponse> getArticles(Integer categoryId, String keyword, String sort, int page, int size);

	// 文章詳情（瀏覽數 +1）
	ArticleResponse getArticle(Long articleId);

	// 編輯自己的文章
	ArticleResponse updateArticle(Long memberId, Long articleId, UpdateArticleRequest request);

	// 刪除（軟刪除）自己的文章
	void deleteArticle(Long memberId, Long articleId);

	// ---------------後台審核功能（暫無權限檢查）------------------

	// 審核／下架／置頂
	ArticleResponse updateArticleStatus(Long articleId, UpdateArticleStatusRequest request);
}
