package com.gigafix.support.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// 打 Bedrock InvokeModel 用的請求格式，是 Anthropic 原生的 Messages API 格式(不是 OpenAI 相容格式)
record AnthropicMessagesRequest(
		@JsonProperty("anthropic_version") String anthropicVersion,
		@JsonProperty("max_tokens") int maxTokens,
		double temperature,
		String system,
		List<AnthropicMessage> messages) {

	record AnthropicMessage(String role, String content) {
	}
}