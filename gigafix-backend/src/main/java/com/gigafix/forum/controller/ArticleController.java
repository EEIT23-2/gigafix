package com.gigafix.forum.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
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

	// 文章詳情（公開，瀏覽數 +1）
	@GetMapping("/api/articles/{articleId}")
	public ResponseEntity<ArticleResponse> getArticle(@PathVariable Long articleId) {

		ArticleResponse response = articleService.getArticle(articleId);

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

	// 審核／下架／置頂
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/articles/{articleId}/status")
	public ResponseEntity<ArticleResponse> updateArticleStatus(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleStatusRequest request) {

		ArticleResponse response = articleService.updateArticleStatus(articleId, request);

		return ResponseEntity.ok(response);
	}
}
