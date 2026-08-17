package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.CommentResponse;
import com.gigafix.forum.dto.CreateCommentRequest;
import com.gigafix.forum.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 留言 Controller
 * 提供留言相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

	// 留言 Service
	private final CommentService commentService;

	// 查詢文章底下所有留言（公開）
	@GetMapping("/api/articles/{articleId}/comments")
	public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long articleId) {

		List<CommentResponse> responses = commentService.getComments(articleId);

		return ResponseEntity.ok(responses);
	}

	// 留言
	@PostMapping("/api/members/{memberId}/articles/{articleId}/comments")
	public ResponseEntity<CommentResponse> createComment(
			@PathVariable Long memberId,
			@PathVariable Long articleId,
			@Valid @RequestBody CreateCommentRequest request) {

		CommentResponse response = commentService.createComment(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 刪除（軟刪除）自己的留言
	@DeleteMapping("/api/members/{memberId}/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(
			@PathVariable Long memberId,
			@PathVariable Long commentId) {

		commentService.deleteComment(memberId, commentId);

		return ResponseEntity.noContent().build();
	}
}
