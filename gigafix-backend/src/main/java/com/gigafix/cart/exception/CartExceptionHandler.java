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
import com.gigafix.user.exception.MemberNotFoundException;

@RestControllerAdvice(basePackages = "com.gigafix.cart.controller")
public class CartExceptionHandler {

	@ExceptionHandler(MemberNotFoundException.class)
	public ResponseEntity<CartErrorResponse> handleMemberNotFound(
			MemberNotFoundException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

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

	@ExceptionHandler(InvalidCartQuantityException.class)
	public ResponseEntity<CartErrorResponse> handleInvalidCartQuantity(
			InvalidCartQuantityException exception,
			HttpServletRequest request
	) {
		return buildErrorResponse(
				HttpStatus.BAD_REQUEST,
				exception.getMessage(),
				request.getRequestURI()
		);
	}

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
