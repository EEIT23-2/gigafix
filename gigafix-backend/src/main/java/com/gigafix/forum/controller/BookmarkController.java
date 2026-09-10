package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.gigafix.common.util.SecurityUtils;
import com.gigafix.forum.dto.BookmarkResponse;
import com.gigafix.forum.service.BookmarkService;

import lombok.RequiredArgsConstructor;

/**
 * 收藏 Controller
 * 提供收藏相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class BookmarkController {

	// 收藏 Service
	private final BookmarkService bookmarkService;

	// 收藏文章
	@PostMapping("/api/members/me/articles/{articleId}/bookmark")
	public ResponseEntity<BookmarkResponse> addBookmark(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		BookmarkResponse response = bookmarkService.addBookmark(memberId, articleId);

		return ResponseEntity.ok(response);
	}

	// 取消收藏
	@DeleteMapping("/api/members/me/articles/{articleId}/bookmark")
	public ResponseEntity<Void> removeBookmark(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		bookmarkService.removeBookmark(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 查詢自己收藏的文章列表
	@GetMapping("/api/members/me/bookmarks")
	public ResponseEntity<List<BookmarkResponse>> getBookmarks(Authentication authentication) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		List<BookmarkResponse> responses = bookmarkService.getBookmarks(memberId);

		return ResponseEntity.ok(responses);
	}

	// 查詢會員是否已收藏某篇文章
	@GetMapping("/api/members/me/articles/{articleId}/bookmark")
	public ResponseEntity<Boolean> hasBookmarked(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(bookmarkService.hasBookmarked(memberId, articleId));
	}
}
