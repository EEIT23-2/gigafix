package com.gigafix.support.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record AnthropicMessagesResponse(List<ContentBlock> content) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ContentBlock(String type, String text) {
	}
}