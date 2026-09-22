package com.gigafix.support.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.gigafix.support.dto.ChatRequest;
import com.gigafix.support.dto.ChatResponse;
import com.gigafix.support.dto.ChatTurnDto;
import com.gigafix.support.exception.SupportChatException;
import com.gigafix.support.service.GroqChatCompletionRequest.GroqMessage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatServiceImpl implements SupportChatService {

	// 前端傳回來的對話紀錄只取最近幾輪，避免有人塞一個超長的假歷史把 token 用量炸掉——
	// 伺服器不信任前端傳來的陣列長度，這裡自己再夾一次上限
	private static final int MAX_HISTORY_TURNS = 10;
	private static final double TEMPERATURE = 0.3;
	private static final int MAX_OUTPUT_TOKENS = 400;

	// 用 GroqRestTemplateConfig 那顆有設逾時的，不是 RestConfig 給匯率/reCAPTCHA/綠界共用的那顆
	@Qualifier("groqRestTemplate")
	private final RestTemplate restTemplate;
	private final SupportChatRateLimiter rateLimiter;

	@Value("${support.chat.api-key}")
	private String apiKey;

	@Value("${support.chat.api-base-url}")
	private String apiBaseUrl;

	@Value("${support.chat.model}")
	private String model;

	// 環境變數只要「有定義」就算解析成功，所以設成空字串時 Spring 會正常啟動，
	// 要等到有人按下送出、上游回 401 才會發現——那時前端只看得到一個沒有前因後果的 502。
	// 這裡在啟動階段就擋下來，讓部署漏設/貼錯金鑰直接反映在容器起不來，而不是變成單一模組的神祕故障。
	// 同時印出長度（不印值）方便比對雲端拿到的金鑰有沒有被截斷或夾帶空白。
	@PostConstruct
	void validateSupportChatConfig() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
					"環境變數 AWS_BEARER_TOKEN_BEDROCK 是空的，AI 客服無法運作，請設定有效的 API Key");
		}
		log.info("AI 客服設定載入完成 model={} baseUrl={} apiKeyLength={}",
				model, apiBaseUrl, apiKey.length());
	}

	@Override
	public ChatResponse chat(Long memberId, ChatRequest request) {
		rateLimiter.checkAndIncrement(memberId);

		List<GroqMessage> messages = new ArrayList<>();
		messages.add(new GroqMessage("system", SupportSystemPrompt.FAQ_SYSTEM_PROMPT));
		messages.addAll(buildHistoryMessages(request.history()));
		messages.add(new GroqMessage("user", request.message()));

		GroqChatCompletionRequest chatRequest = new GroqChatCompletionRequest(
				model, messages, TEMPERATURE, MAX_OUTPUT_TOKENS);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		GroqChatCompletionResponse response;
		try {
			response = restTemplate.postForObject(
					apiBaseUrl + "/chat/completions",
					new HttpEntity<>(chatRequest, headers),
					GroqChatCompletionResponse.class);
		} catch (HttpStatusCodeException e) {
			// 上游有回應、但不是 2xx：401 金鑰不對、403 來源被擋、429 額度用完、404 模型不存在都長這樣，
			// 回應 body 會寫明原因。這裡一定要記下來，不然部署環境出事時外面只看得到一個沒有線索的 502。
			// 注意：只記狀態碼與 body，絕對不要把 apiKey 印進日誌
			log.error("呼叫 AI 客服上游失敗，上游回應 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
			throw SupportChatException.upstreamError("客服服務暫時無法使用，請稍後再試");
		} catch (RestClientException e) {
			// 連線被拒、DNS 解不到、逾時、回應解析失敗等「根本沒拿到完整回應」的情況
			log.error("呼叫 AI 客服上游失敗，無法取得回應", e);
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
