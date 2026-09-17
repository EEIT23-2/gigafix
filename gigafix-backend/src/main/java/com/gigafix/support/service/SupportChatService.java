package com.gigafix.support.service;

import com.gigafix.support.dto.ChatRequest;
import com.gigafix.support.dto.ChatResponse;

public interface SupportChatService {

	ChatResponse chat(Long memberId, ChatRequest request);
}
