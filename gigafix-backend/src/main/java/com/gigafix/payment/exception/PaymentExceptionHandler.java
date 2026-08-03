package com.gigafix.payment.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.gigafix.payment.controller")
/** 將付款模組例外轉為一致且不洩漏內部資訊的 API 錯誤。 */
public class PaymentExceptionHandler {
	@ExceptionHandler(PaymentNotFoundException.class)
	ResponseEntity<PaymentErrorResponse> notFound(RuntimeException ex, HttpServletRequest req) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
	}

	@ExceptionHandler({DuplicatePaymentException.class, InvalidPaymentOperationException.class,
			InvalidPaymentStatusTransitionException.class})
	ResponseEntity<PaymentErrorResponse> conflict(RuntimeException ex, HttpServletRequest req) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), req);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<PaymentErrorResponse> badRequest(RuntimeException ex, HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<PaymentErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed");
		return build(HttpStatus.BAD_REQUEST, message, req);
	}

	private ResponseEntity<PaymentErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
		return ResponseEntity.status(status).body(new PaymentErrorResponse(
				LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI()
		));
	}
}
