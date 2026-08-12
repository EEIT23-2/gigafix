package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidOldPasswordException extends MemberException {
	public InvalidOldPasswordException() {
        super("INVALID_CREDENTIALS", "帳號或密碼錯誤", HttpStatus.UNAUTHORIZED); // 401
    }
}
