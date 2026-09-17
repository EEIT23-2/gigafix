package com.gigafix.forum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.forum.dto.ForumSeedResponse;
import com.gigafix.forum.service.ForumSeedService;

import lombok.RequiredArgsConstructor;

/**
 * 論壇展示資料 Controller
 * 提供後台「一鍵產生展示資料」使用
 */
@RestController
@RequiredArgsConstructor
public class ForumSeedController {

	// 展示資料 Service
	private final ForumSeedService forumSeedService;

	// 產生展示資料（分類、文章、樓層、留言、按讚）
	// 已經產生過會回 409，資料庫沒有任何會員會回 400
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@PostMapping("/api/admin/forum/seed")
	public ResponseEntity<ForumSeedResponse> seedForumData() {

		ForumSeedResponse response = forumSeedService.seedForumData();

		return ResponseEntity.ok(response);
	}
}
