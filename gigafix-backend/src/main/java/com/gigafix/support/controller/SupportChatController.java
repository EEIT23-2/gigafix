package com.gigafix.support.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.common.util.SecurityUtils;
import com.gigafix.member.security.MemberUserDetails;
import com.gigafix.support.dto.ChatRequest;
import com.gigafix.support.dto.ChatResponse;
import com.gigafix.support.service.SupportChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 這支路徑落在 SecurityConfig 的 memberSecurityFilterChain、anyRequest().authenticated() 範圍內，
// 沒有加進 MemberPublicApiPaths，所以天生就只有登入會員能打，不用額外加權限檢查
@RestController
@RequestMapping("/api/gigafix/support")
@RequiredArgsConstructor
public class SupportChatController {

	private final SupportChatService supportChatService;

	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest chatRequest,
			Authentication authentication) {
		MemberUserDetails memberDetails = SecurityUtils.getCurrentMember(authentication);
		ChatResponse response = supportChatService.chat(memberDetails.getId(), chatRequest);
		return ResponseEntity.ok(response);
	}
}
