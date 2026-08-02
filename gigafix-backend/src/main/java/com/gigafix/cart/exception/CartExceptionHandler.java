package com.gigafix.cart.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import com.gigafix.order.exception.OrderMemberNotFoundException;

/**
 * 購物車 Controller 的統一例外處理器。
 * 將 Cart Service、結帳流程與請求驗證產生的例外轉為前端可理解的 CartErrorResponse。
 */
@RestControllerAdvice(basePackages = "com.gigafix.cart.controller")
public class CartExceptionHandler {

	/**
	 * 將購物車或結帳流程的會員不存在例外轉為 404 Not Found。
	 */
	@ExceptionHandler({
			CartMemberNotFoundException.class,
			OrderMemberNotFoundException.class
	})
	public ResponseEntity<CartErrorResponse> handleMemberNotFound(
			RuntimeException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將啟用中購物車不存在例外轉為 404 Not Found。
	 */
	@ExceptionHandler(CartNotFoundException.class)
	public ResponseEntity<CartErrorResponse> handleCartNotFound(
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
	public ResponseEntity<CartErrorResponse> handleEmptyCart(
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
	 * 將商品資料尚未完成而無法結帳的例外轉為 501 Not Implemented。
	 */
	@ExceptionHandler(CheckoutNotAvailableException.class)
	public ResponseEntity<CartErrorResponse> handleCheckoutNotAvailable(
			CheckoutNotAvailableException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_IMPLEMENTED,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將結帳期間發生的資料鎖定或版本衝突轉為 409 Conflict。
	 */
	@ExceptionHandler({
			OptimisticLockException.class,
			PessimisticLockException.class,
			OptimisticLockingFailureException.class,
			PessimisticLockingFailureException.class
	})
	public ResponseEntity<CartErrorResponse> handleCheckoutLockConflict(
			RuntimeException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.CONFLICT,
				"購物車正在結帳，請稍後再試",
				request.getRequestURI()
		);
	}

	/**
	 * 將購物車項目不存在或不屬於會員的例外轉為 404 Not Found。
	 */
	@ExceptionHandler(CartItemNotFoundException.class)
	public ResponseEntity<CartErrorResponse> handleCartItemNotFound(
			CartItemNotFoundException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將重複商品或非 ACTIVE 購物車修改轉為 409 Conflict。
	 * TODO 下一輪：若資料庫已有 (cart_id, product_id) unique constraint，
	 * 將重複寫入產生的 DataIntegrityViolationException 安全映射為 409。
	 */
	@ExceptionHandler({
			DuplicateCartItemException.class,
			CartNotModifiableException.class
	})
	public ResponseEntity<CartErrorResponse> handleCartConflict(
			RuntimeException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.CONFLICT,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

	/**
	 * 將識別碼或其他方法參數錯誤轉為 400 Bad Request。
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<CartErrorResponse> handleIllegalArgument(
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
	public ResponseEntity<CartErrorResponse> handleValidation(
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

	private ResponseEntity<CartErrorResponse> buildErrorResponse(
			HttpStatus status,
			String message,
			String path
	) {
		CartErrorResponse response = new CartErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path
		);

		return ResponseEntity.status(status).body(response);
	}
}
