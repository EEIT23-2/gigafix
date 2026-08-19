package com.gigafix.forum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.LikeResponse;
import com.gigafix.forum.service.LikeService;

import lombok.RequiredArgsConstructor;

/**
 * 讚 Controller
 * 提供按讚相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class LikeController {

	// 讚 Service
	private final LikeService likeService;

	// 對文章按讚
	@PostMapping("/api/members/{memberId}/articles/{articleId}/like")
	public ResponseEntity<LikeResponse> likeArticle(
			@PathVariable Long memberId,
			@PathVariable Long articleId) {

		LikeResponse response = likeService.likeArticle(memberId, articleId);

		return ResponseEntity.ok(response);
	}

	// 取消對文章的讚
	@DeleteMapping("/api/members/{memberId}/articles/{articleId}/like")
	public ResponseEntity<Void> unlikeArticle(
			@PathVariable Long memberId,
			@PathVariable Long articleId) {

		likeService.unlikeArticle(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 對留言按讚
	@PostMapping("/api/members/{memberId}/comments/{commentId}/like")
	public ResponseEntity<LikeResponse> likeComment(
			@PathVariable Long memberId,
			@PathVariable Long commentId) {

		LikeResponse response = likeService.likeComment(memberId, commentId);

		return ResponseEntity.ok(response);
	}

	// 取消對留言的讚
	@DeleteMapping("/api/members/{memberId}/comments/{commentId}/like")
	public ResponseEntity<Void> unlikeComment(
			@PathVariable Long memberId,
			@PathVariable Long commentId) {

		likeService.unlikeComment(memberId, commentId);

		return ResponseEntity.noContent().build();
	}

	// 查詢會員是否已對某篇文章按讚
	@GetMapping("/api/members/{memberId}/articles/{articleId}/like")
	public ResponseEntity<Boolean> hasLikedArticle(
			@PathVariable Long memberId,
			@PathVariable Long articleId) {

		return ResponseEntity.ok(likeService.hasLikedArticle(memberId, articleId));
	}

	// 查詢會員是否已對某則留言按讚
	@GetMapping("/api/members/{memberId}/comments/{commentId}/like")
	public ResponseEntity<Boolean> hasLikedComment(
			@PathVariable Long memberId,
			@PathVariable Long commentId) {

		return ResponseEntity.ok(likeService.hasLikedComment(memberId, commentId));
	}
}
