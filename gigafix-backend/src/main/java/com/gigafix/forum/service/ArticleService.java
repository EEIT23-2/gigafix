package com.gigafix.forum.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.CreateFloorRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
import com.gigafix.forum.dto.UpdateFloorRequest;
import com.gigafix.forum.entity.Article;

/**
 * 文章 Service
 * 定義文章相關商業功能
 */
public interface ArticleService {

	// ---------------會員前台功能------------------

	// 發文（status 選填，預設 PUBLISHED；DRAFT 供草稿自動儲存流程使用）
	ArticleResponse createArticle(Long memberId, CreateArticleRequest request);

	// 文章列表（分類篩選、關鍵字搜尋、排序、分頁），只回傳可公開瀏覽的文章、排除樓層
	Page<ArticleResponse> getArticles(Integer categoryId, String keyword, String sort, int page, int size);

	// 文章詳情（瀏覽數 +1；memberId 選填，用來判斷 DRAFT/HIDDEN/FORCE_HIDDEN 是否為作者本人）
	ArticleResponse getArticle(Long articleId, Long memberId);

	// 編輯自己的文章
	ArticleResponse updateArticle(Long memberId, Long articleId, UpdateArticleRequest request);

	// 刪除（軟刪除）自己的文章
	void deleteArticle(Long memberId, Long articleId);

	// 會員自行變更自己文章的狀態（發布/隱藏/下架/關閉）
	ArticleResponse updateOwnArticleStatus(Long memberId, Long articleId, UpdateArticleStatusRequest request);

	// 會員自己的文章列表（個人中心用，含草稿/隱藏，排除下架）
	List<ArticleResponse> getMyArticles(Long memberId);

	// 捨棄草稿：真的把列刪掉。只接受 DRAFT，其餘狀態一律拒絕
	// （deleteArticle 是軟刪除，會留下 TAKEN_DOWN 的列；從未公開過的草稿不該以「下架」的身分留在稽核清單裡）
	void deleteDraft(Long memberId, Long articleId);

	// 樓層列表（memberId 選填，套用與文章詳情相同的可見性規則）
	List<ArticleResponse> getFloors(Long articleId, Long memberId);

	// 蓋樓
	ArticleResponse createFloor(Long memberId, Long articleId, CreateFloorRequest request);

	// 編輯樓層（只改內文；標題與分類仍由後端擁有）
	ArticleResponse updateFloor(Long memberId, Long floorId, UpdateFloorRequest request);

	// ---------------後台管理功能（暫無權限檢查）------------------

	// 後台文章列表，不受狀態限制
	Page<ArticleResponse> getArticlesForAdmin(Article.ArticleStatus status, Integer categoryId, String keyword,
			Long authorId, int page, int size);

	// 後台文章詳情，不受狀態限制，完整內容
	ArticleResponse getArticleForAdmin(Long articleId);

	// 審核／強制處分（1~6 皆可）
	ArticleResponse updateArticleStatus(Long articleId, UpdateArticleStatusRequest request);

	// 置頂／取消置頂
	ArticleResponse updateArticlePin(Long articleId, Boolean isPinned);
}
