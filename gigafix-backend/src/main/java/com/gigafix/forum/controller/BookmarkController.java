package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
	@PostMapping("/api/members/{memberId}/articles/{articleId}/bookmark")
	public ResponseEntity<BookmarkResponse> addBookmark(
			@PathVariable Long memberId,
			@PathVariable Long articleId) {

		BookmarkResponse response = bookmarkService.addBookmark(memberId, articleId);

		return ResponseEntity.ok(response);
	}

	// 取消收藏
	@DeleteMapping("/api/members/{memberId}/articles/{articleId}/bookmark")
	public ResponseEntity<Void> removeBookmark(
			@PathVariable Long memberId,
			@PathVariable Long articleId) {

		bookmarkService.removeBookmark(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 查詢自己收藏的文章列表
	@GetMapping("/api/members/{memberId}/bookmarks")
	public ResponseEntity<List<BookmarkResponse>> getBookmarks(@PathVariable Long memberId) {

		List<BookmarkResponse> responses = bookmarkService.getBookmarks(memberId);

		return ResponseEntity.ok(responses);
	}
}
