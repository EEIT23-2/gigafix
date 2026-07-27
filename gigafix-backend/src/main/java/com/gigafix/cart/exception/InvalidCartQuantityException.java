package com.gigafix.cart.exception;

public class InvalidCartQuantityException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidCartQuantityException(String message) {
		super(message);
	}
}
