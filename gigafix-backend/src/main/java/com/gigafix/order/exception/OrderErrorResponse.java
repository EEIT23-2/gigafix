package com.gigafix.order.exception;

import java.time.LocalDateTime;

/**
 * 訂單 API 發生錯誤時回傳給前端的統一格式。
 * 由 OrderExceptionHandler 組合 HTTP 狀態、訊息、請求路徑與發生時間。
 */
public record OrderErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path
) {
}
