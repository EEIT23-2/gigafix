package com.gigafix.order.exception;

import java.time.LocalDateTime;

public record OrderErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path
) {
}
