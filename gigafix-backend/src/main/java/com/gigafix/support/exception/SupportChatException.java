package com.gigafix.support.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

// AI 客服模組的自訂例外，比照 ForumException 的 errorCode + HttpStatus 寫法
@Getter
public class SupportChatException extends RuntimeException {

	private final String errorCode;
	private final HttpStatus httpStatus;

	private SupportChatException(String errorCode, String message, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	// 超過限流次數（429）
	public static SupportChatException rateLimited(String message) {
		return new SupportChatException("SUPPORT_CHAT_RATE_LIMITED", message, HttpStatus.TOO_MANY_REQUESTS);
	}

	// 呼叫 Groq 失敗（逾時、非 2xx、解析不到回覆等）（502）
	public static SupportChatException upstreamError(String message) {
		return new SupportChatException("SUPPORT_CHAT_UPSTREAM_ERROR", message, HttpStatus.BAD_GATEWAY);
	}

	// 參數驗證失敗，例如 history 帶了不合法的 role（400）
	public static SupportChatException invalidInput(String message) {
		return new SupportChatException("SUPPORT_CHAT_INVALID_INPUT", message, HttpStatus.BAD_REQUEST);
	}
}
