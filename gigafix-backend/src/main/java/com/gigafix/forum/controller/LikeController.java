package com.gigafix.forum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.gigafix.common.util.SecurityUtils;
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
	@PostMapping("/api/members/me/articles/{articleId}/like")
	public ResponseEntity<LikeResponse> likeArticle(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		LikeResponse response = likeService.likeArticle(memberId, articleId);

		return ResponseEntity.ok(response);
	}

	// 取消對文章的讚
	@DeleteMapping("/api/members/me/articles/{articleId}/like")
	public ResponseEntity<Void> unlikeArticle(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		likeService.unlikeArticle(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 對留言按讚
	@PostMapping("/api/members/me/comments/{commentId}/like")
	public ResponseEntity<LikeResponse> likeComment(
			Authentication authentication,
			@PathVariable Long commentId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		LikeResponse response = likeService.likeComment(memberId, commentId);

		return ResponseEntity.ok(response);
	}

	// 取消對留言的讚
	@DeleteMapping("/api/members/me/comments/{commentId}/like")
	public ResponseEntity<Void> unlikeComment(
			Authentication authentication,
			@PathVariable Long commentId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		likeService.unlikeComment(memberId, commentId);

		return ResponseEntity.noContent().build();
	}

	// 查詢會員是否已對某篇文章按讚
	@GetMapping("/api/members/me/articles/{articleId}/like")
	public ResponseEntity<Boolean> hasLikedArticle(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(likeService.hasLikedArticle(memberId, articleId));
	}

	// 查詢會員是否已對某則留言按讚
	@GetMapping("/api/members/me/comments/{commentId}/like")
	public ResponseEntity<Boolean> hasLikedComment(
			Authentication authentication,
			@PathVariable Long commentId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(likeService.hasLikedComment(memberId, commentId));
	}
}
