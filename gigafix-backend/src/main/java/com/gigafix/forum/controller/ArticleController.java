package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.CreateFloorRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
import com.gigafix.forum.dto.UpdatePinRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.service.ArticleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 文章 Controller
 * 提供文章相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class ArticleController {

	// 文章 Service
	private final ArticleService articleService;

	// 文章列表（公開）
	@GetMapping("/api/articles")
	public ResponseEntity<Page<ArticleResponse>> getArticles(
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "latest") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Page<ArticleResponse> response = articleService.getArticles(categoryId, keyword, sort, page, size);

		return ResponseEntity.ok(response);
	}

	// 文章詳情（公開，瀏覽數 +1；memberId 選填，用來判斷草稿/隱藏狀態是否為作者本人）
	@GetMapping("/api/articles/{articleId}")
	public ResponseEntity<ArticleResponse> getArticle(
			@PathVariable Long articleId,
			@RequestParam(required = false) Long memberId) {

		ArticleResponse response = articleService.getArticle(articleId, memberId);

		return ResponseEntity.ok(response);
	}

	// 發文
	@PostMapping("/api/members/{memberId}/articles")
	public ResponseEntity<ArticleResponse> createArticle(
			@PathVariable Long memberId,
			@Valid @RequestBody CreateArticleRequest request) {

		ArticleResponse response = articleService.createArticle(memberId, request);

		return ResponseEntity.ok(response);
	}

	// 編輯自己的文章
	@PutMapping("/api/members/{memberId}/articles/{articleId}")
	public ResponseEntity<ArticleResponse> updateArticle(
			@PathVariable Long memberId,
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleRequest request) {

		ArticleResponse response = articleService.updateArticle(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 刪除（軟刪除）自己的文章
	@DeleteMapping("/api/members/{memberId}/articles/{articleId}")
	public ResponseEntity<Void> deleteArticle(
			@PathVariable Long memberId,
			@PathVariable Long articleId) {

		articleService.deleteArticle(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 會員自行變更自己文章的狀態（發布/隱藏/下架/關閉）
	@PatchMapping("/api/members/{memberId}/articles/{articleId}/status")
	public ResponseEntity<ArticleResponse> updateOwnArticleStatus(
			@PathVariable Long memberId,
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleStatusRequest request) {

		ArticleResponse response = articleService.updateOwnArticleStatus(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 會員自己的文章列表（個人中心）
	@GetMapping("/api/members/{memberId}/articles")
	public ResponseEntity<List<ArticleResponse>> getMyArticles(@PathVariable Long memberId) {

		List<ArticleResponse> responses = articleService.getMyArticles(memberId);

		return ResponseEntity.ok(responses);
	}

	// 樓層列表（公開；memberId 選填，套用與文章詳情相同的可見性規則）
	@GetMapping("/api/articles/{articleId}/floors")
	public ResponseEntity<List<ArticleResponse>> getFloors(
			@PathVariable Long articleId,
			@RequestParam(required = false) Long memberId) {

		List<ArticleResponse> responses = articleService.getFloors(articleId, memberId);

		return ResponseEntity.ok(responses);
	}

	// 蓋樓
	@PostMapping("/api/members/{memberId}/articles/{articleId}/floors")
	public ResponseEntity<ArticleResponse> createFloor(
			@PathVariable Long memberId,
			@PathVariable Long articleId,
			@Valid @RequestBody CreateFloorRequest request) {

		ArticleResponse response = articleService.createFloor(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// ---------------後台管理功能----------------------

	// 後台文章列表，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/articles")
	public ResponseEntity<Page<ArticleResponse>> getArticlesForAdmin(
			@RequestParam(required = false) Article.ArticleStatus status,
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long authorId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Page<ArticleResponse> response = articleService.getArticlesForAdmin(status, categoryId, keyword, authorId,
				page, size);

		return ResponseEntity.ok(response);
	}

	// 後台文章詳情，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/articles/{articleId}")
	public ResponseEntity<ArticleResponse> getArticleForAdmin(@PathVariable Long articleId) {

		ArticleResponse response = articleService.getArticleForAdmin(articleId);

		return ResponseEntity.ok(response);
	}

	// 審核／下架／強制處分
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/admin/articles/{articleId}/status")
	public ResponseEntity<ArticleResponse> updateArticleStatus(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleStatusRequest request) {

		ArticleResponse response = articleService.updateArticleStatus(articleId, request);

		return ResponseEntity.ok(response);
	}

	// 置頂／取消置頂
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/admin/articles/{articleId}/pin")
	public ResponseEntity<ArticleResponse> updateArticlePin(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdatePinRequest request) {

		ArticleResponse response = articleService.updateArticlePin(articleId, request.getIsPinned());

		return ResponseEntity.ok(response);
	}
}
