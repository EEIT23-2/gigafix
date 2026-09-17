package com.gigafix.support.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Groq 的 chat completions 回應格式（OpenAI 相容格式），只解析我們需要的欄位，其餘忽略
@JsonIgnoreProperties(ignoreUnknown = true)
record GroqChatCompletionResponse(List<Choice> choices) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Choice(Message message) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Message(String role, String content) {
	}
}
