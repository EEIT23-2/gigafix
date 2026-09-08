package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.gigafix.common.util.SecurityUtils;
import com.gigafix.forum.dto.CommentResponse;
import com.gigafix.forum.dto.CreateCommentRequest;
import com.gigafix.forum.dto.UpdateCommentStatusRequest;
import com.gigafix.forum.service.CommentService;
import com.gigafix.member.security.MemberUserDetails;

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

	// 查詢文章底下所有留言（公開；有登入的話會附上每則留言是否已被該會員按讚、是否為本人所發）
	@GetMapping("/api/articles/{articleId}/comments")
	public ResponseEntity<List<CommentResponse>> getComments(
			@PathVariable Long articleId,
			Authentication authentication) {

		List<CommentResponse> responses = commentService.getComments(articleId, currentMemberId(authentication));

		return ResponseEntity.ok(responses);
	}

	// 留言
	@PostMapping("/api/members/me/articles/{articleId}/comments")
	public ResponseEntity<CommentResponse> createComment(
			Authentication authentication,
			@PathVariable Long articleId,
			@Valid @RequestBody CreateCommentRequest request) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		CommentResponse response = commentService.createComment(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 刪除（軟刪除）自己的留言
	@DeleteMapping("/api/members/me/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(
			Authentication authentication,
			@PathVariable Long commentId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		commentService.deleteComment(memberId, commentId);

		return ResponseEntity.noContent().build();
	}

	// 會員自己的留言歷史（個人中心）
	@GetMapping("/api/members/me/comments")
	public ResponseEntity<List<CommentResponse>> getMyComments(Authentication authentication) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		List<CommentResponse> responses = commentService.getMyComments(memberId);

		return ResponseEntity.ok(responses);
	}

	// 後台單筆留言詳情，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/comments/{commentId}")
	public ResponseEntity<CommentResponse> getCommentForAdmin(@PathVariable Long commentId) {

		CommentResponse response = commentService.getCommentForAdmin(commentId);

		return ResponseEntity.ok(response);
	}

	// 後台的文章留言串：不受狀態限制，已下架的留言也會回傳（前台版本會過濾掉）
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/articles/{articleId}/comments")
	public ResponseEntity<List<CommentResponse>> getCommentsForAdmin(@PathVariable Long articleId) {

		List<CommentResponse> responses = commentService.getCommentsForAdmin(articleId);

		return ResponseEntity.ok(responses);
	}

	// 後台直接設定留言狀態（隱藏／下架／恢復）
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/admin/comments/{commentId}/status")
	public ResponseEntity<CommentResponse> updateCommentStatus(
			@PathVariable Long commentId,
			@Valid @RequestBody UpdateCommentStatusRequest request) {

		CommentResponse response = commentService.updateCommentStatus(commentId, request);

		return ResponseEntity.ok(response);
	}

	// 從 Authentication 解出目前登入的 memberId；未登入（匿名 principal）時回傳 null
	private Long currentMemberId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof MemberUserDetails details) {
			return details.getId();
		}
		return null;
	}
}
