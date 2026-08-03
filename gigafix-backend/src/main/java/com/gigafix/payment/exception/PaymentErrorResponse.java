package com.gigafix.payment.exception;

import java.time.LocalDateTime;

/** 統一付款 API 的錯誤回應格式。 */
public record PaymentErrorResponse(
		LocalDateTime timestamp, int status, String error, String message, String path
) {
}
