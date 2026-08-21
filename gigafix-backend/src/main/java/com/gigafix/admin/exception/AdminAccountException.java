package com.gigafix.admin.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class AdminAccountException extends RuntimeException {
	private final String errorCode;
	private final HttpStatus httpStatus;
	
	//使用protected，因為只會給member package內要自訂義的exception 繼承
	protected AdminAccountException (String errorCode, String message, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}
}
