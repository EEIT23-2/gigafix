package com.gigafix.cart.exception;

import java.time.LocalDateTime;

/**
 * 購物車 API 發生錯誤時回傳給前端的統一格式。
 * 由 CartExceptionHandler 組合 HTTP 狀態、訊息、請求路徑與發生時間。
 */
public record CartErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path
) {
}
