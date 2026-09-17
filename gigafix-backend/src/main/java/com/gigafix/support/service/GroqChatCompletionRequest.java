package com.gigafix.support.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// 送給 Groq 的 chat completions 請求格式（OpenAI 相容格式）
record GroqChatCompletionRequest(
		String model,
		List<GroqMessage> messages,
		double temperature,
		@JsonProperty("max_tokens") int maxTokens) {

	record GroqMessage(String role, String content) {
	}
}
