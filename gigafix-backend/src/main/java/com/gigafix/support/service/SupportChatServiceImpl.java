package com.gigafix.support.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigafix.support.dto.ChatRequest;
import com.gigafix.support.dto.ChatResponse;
import com.gigafix.support.dto.ChatTurnDto;
import com.gigafix.support.exception.SupportChatException;
import com.gigafix.support.service.AnthropicMessagesRequest.AnthropicMessage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatServiceImpl implements SupportChatService {

	// Bedrock 呼叫 Anthropic 模型時 InvokeModel body 固定要帶這個版本字串，跟你選的模型版本無關，是 API 規格要求的
	private static final String ANTHROPIC_VERSION = "bedrock-2023-05-31";

	private static final int MAX_HISTORY_TURNS = 10;
	private static final double TEMPERATURE = 0.3;
	private static final int MAX_OUTPUT_TOKENS = 400;

	private final BedrockRuntimeClient bedrockRuntimeClient;
	private final SupportChatRateLimiter rateLimiter;
	private final ObjectMapper objectMapper;

	@Value("${support.chat.model}")
	private String model;

	@PostConstruct
	void validateSupportChatConfig() {
		if (model == null || model.isBlank()) {
			throw new IllegalStateException(
					"環境變數 SUPPORT_CHAT_MODEL 是空的，AI 客服無法運作，請設定有效的模型/推論設定檔 ID");
		}
		log.info("AI 客服設定載入完成 model={}", model);
	}

	@Override
	public ChatResponse chat(Long memberId, ChatRequest request) {
		rateLimiter.checkAndIncrement(memberId);

		List<AnthropicMessage> messages = buildHistoryMessages(request.history());
		messages.add(new AnthropicMessage("user", request.message()));

		AnthropicMessagesRequest chatRequest = new AnthropicMessagesRequest(
				ANTHROPIC_VERSION, MAX_OUTPUT_TOKENS, TEMPERATURE, SupportSystemPrompt.FAQ_SYSTEM_PROMPT, messages);

		InvokeModelRequest invokeModelRequest;
		try {
			invokeModelRequest = InvokeModelRequest.builder()
					.modelId(model)
					.contentType("application/json")
					.body(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(chatRequest)))
					.build();
		} catch (JsonProcessingException e) {
			log.error("組裝 AI 客服請求失敗", e);
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}

		InvokeModelResponse response;
		try {
			response = bedrockRuntimeClient.invokeModel(invokeModelRequest);
		} catch (BedrockRuntimeException e) {
			// 上游有回應、但不是成功：AccessDenied 權限不對、ValidationException 參數/模型不對、
			// ThrottlingException 額度用完都長這樣。注意：只記狀態碼與錯誤訊息，絕對不要把金鑰印進日誌
			log.error("呼叫 AI 客服上游失敗，上游回應 status={} message={}", e.statusCode(), e.getMessage());
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		} catch (SdkClientException e) {
			log.error("呼叫 AI 客服上游失敗，無法取得回應", e);
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}

		String reply = extractReply(response);
		return new ChatResponse(reply);
	}

	// 只接受 user/assistant 兩種角色（過濾掉前端萬一夾帶的 system role），並夾住最近幾輪的數量上限
	private List<AnthropicMessage> buildHistoryMessages(List<ChatTurnDto> history) {
		if (history == null || history.isEmpty()) {
			return new ArrayList<>();
		}

		List<ChatTurnDto> validTurns = history.stream()
				.filter(turn -> turn != null && ("user".equals(turn.role()) || "assistant".equals(turn.role())))
				.toList();

		int fromIndex = Math.max(0, validTurns.size() - MAX_HISTORY_TURNS);
		return new ArrayList<>(validTurns.subList(fromIndex, validTurns.size()).stream()
				.map(turn -> new AnthropicMessage(turn.role(), turn.content()))
				.toList());
	}

	private String extractReply(InvokeModelResponse response) {
		AnthropicMessagesResponse parsed;
		try {
			parsed = objectMapper.readValue(response.body().asUtf8String(), AnthropicMessagesResponse.class);
		} catch (JsonProcessingException e) {
			log.error("解析 AI 客服回應失敗", e);
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}
		if (parsed == null || parsed.content() == null || parsed.content().isEmpty()) {
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}
		String content = parsed.content().get(0).text();
		if (content == null || content.isBlank()) {
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		}
		return content;
	}
}