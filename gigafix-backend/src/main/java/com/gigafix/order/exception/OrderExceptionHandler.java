package com.gigafix.order.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.EmptyCartException;

/**
 * 訂單 Controller 的統一例外處理器。
 * 將 Order Service、結帳前置檢查與請求驗證產生的例外轉為前端可理解的 OrderErrorResponse。
 */
@RestControllerAdvice(basePackages = "com.gigafix.order.controller")
public class OrderExceptionHandler {

	/**
	 * 將會員不存在例外轉為 404 Not Found。
	 */
	@ExceptionHandler(OrderMemberNotFoundException.class)
	public ResponseEntity<OrderErrorResponse> handleMemberNotFound(
			OrderMemberNotFoundException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將結帳時找不到啟用中購物車的例外轉為 404 Not Found。
	 */
	@ExceptionHandler(CartNotFoundException.class)
	public ResponseEntity<OrderErrorResponse> handleCartNotFound(
			CartNotFoundException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將空購物車無法結帳的例外轉為 409 Conflict。
	 */
	@ExceptionHandler(EmptyCartException.class)
	public ResponseEntity<OrderErrorResponse> handleEmptyCart(
			EmptyCartException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.CONFLICT,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將訂單不存在或不屬於會員的例外轉為 404 Not Found。
	 */
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

	/**
	 * 將不符合訂單規則的操作轉為 400 Bad Request。
	 */
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

	/**
	 * 將識別碼或其他方法參數錯誤轉為 400 Bad Request。
	 */
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

	/**
	 * 將 Request DTO 驗證失敗轉為 400 Bad Request，並回傳第一個欄位錯誤。
	 */
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
