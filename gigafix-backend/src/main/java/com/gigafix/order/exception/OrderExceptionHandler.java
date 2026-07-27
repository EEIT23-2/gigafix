package com.gigafix.order.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.gigafix.order.controller")
public class OrderExceptionHandler {

	@ExceptionHandler(OrderNotFoundException.class)
	public ResponseEntity<OrderErrorResponse> handleOrderNotFound(
			OrderNotFoundException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	@ExceptionHandler(InvalidOrderException.class)
	public ResponseEntity<OrderErrorResponse> handleInvalidOrder(
			InvalidOrderException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.BAD_REQUEST,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<OrderErrorResponse> handleIllegalArgument(
			IllegalArgumentException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.BAD_REQUEST,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<OrderErrorResponse> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed");

		return buildErrorResponse(
				HttpStatus.BAD_REQUEST,
				message,
				request.getRequestURI()
		);
	}

	private ResponseEntity<OrderErrorResponse> buildErrorResponse(
			HttpStatus status,
			String message,
			String path
	) {
		OrderErrorResponse response = new OrderErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path
		);

		return ResponseEntity.status(status).body(response);
	}
}
