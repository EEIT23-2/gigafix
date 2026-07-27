package com.gigafix.cart.exception;

import java.time.LocalDateTime;

public record CartErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path
) {
}
