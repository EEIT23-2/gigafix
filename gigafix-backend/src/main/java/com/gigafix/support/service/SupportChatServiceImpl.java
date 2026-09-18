package com.gigafix.support.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.gigafix.support.dto.ChatRequest;
import com.gigafix.support.dto.ChatResponse;
import com.gigafix.support.dto.ChatTurnDto;
import com.gigafix.support.exception.SupportChatException;
import com.gigafix.support.service.GroqChatCompletionRequest.GroqMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportChatServiceImpl implements SupportChatService {

	// 前端傳回來的對話紀錄只取最近幾輪，避免有人塞一個超長的假歷史把 token 用量炸掉——
	// 伺服器不信任前端傳來的陣列長度，這裡自己再夾一次上限
	private static final int MAX_HISTORY_TURNS = 10;
	private static final double TEMPERATURE = 0.3;
	private static final int MAX_OUTPUT_TOKENS = 400;

	private final RestTemplate restTemplate; // 沿用RestConfig.java已經註冊好的那顆bean
	private final SupportChatRateLimiter rateLimiter;

	@Value("${groq.api-key}")
	private String groqApiKey;

	@Value("${groq.api-base-url}")
	private String groqApiBaseUrl;

	@Value("${groq.model}")
	private String groqModel;

	@Override
	public ChatResponse chat(Long memberId, ChatRequest request) {
		rateLimiter.checkAndIncrement(memberId);

		List<GroqMessage> messages = new ArrayList<>();
		messages.add(new GroqMessage("system", SupportSystemPrompt.FAQ_SYSTEM_PROMPT));
		messages.addAll(buildHistoryMessages(request.history()));
		messages.add(new GroqMessage("user", request.message()));

		GroqChatCompletionRequest groqRequest = new GroqChatCompletionRequest(
				groqModel, messages, TEMPERATURE, MAX_OUTPUT_TOKENS);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(groqApiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		GroqChatCompletionResponse response;
		try {
			response = restTemplate.postForObject(
					groqApiBaseUrl + "/chat/completions",
					new HttpEntity<>(groqRequest, headers),
					GroqChatCompletionResponse.class);
		} catch (RestClientException e) {
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}

		String reply = extractReply(response);
		return new ChatResponse(reply);
	}

	// 只接受 user/assistant 兩種角色（過濾掉前端萬一夾帶的 system role），並夾住最近幾輪的數量上限
	private List<GroqMessage> buildHistoryMessages(List<ChatTurnDto> history) {
		if (history == null || history.isEmpty()) {
			return List.of();
		}

		List<ChatTurnDto> validTurns = history.stream()
				.filter(turn -> turn != null && ("user".equals(turn.role()) || "assistant".equals(turn.role())))
				.toList();

		int fromIndex = Math.max(0, validTurns.size() - MAX_HISTORY_TURNS);
		return validTurns.subList(fromIndex, validTurns.size()).stream()
				.map(turn -> new GroqMessage(turn.role(), turn.content()))
				.toList();
	}

	private String extractReply(GroqChatCompletionResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}
		String content = response.choices().get(0).message().content();
		if (content == null || content.isBlank()) {
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}
		return content;
	}
}
