package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidCaptchaException extends MemberException {
	public InvalidCaptchaException() {
		super("INVALID_CAPTCHA", "人機驗證失敗，請重新勾選驗證", HttpStatus.BAD_REQUEST); //400
	}
}
