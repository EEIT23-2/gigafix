package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends MemberException {
    public InvalidCredentialsException() {
        super("INVALID_CURRENT_PASSWORD", "帳號密碼輸入錯誤", HttpStatus.UNAUTHORIZED); // 401
    }
}
