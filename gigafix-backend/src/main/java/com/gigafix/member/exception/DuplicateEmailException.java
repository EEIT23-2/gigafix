package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends MemberException {
	public DuplicateEmailException() {
		//直接在子類別建構式寫死資訊，不用每次throw這個錯誤的時候寫1次
		super("DUPLICATE_EMAIL", "此信箱已被註冊", HttpStatus.CONFLICT); //409
	}
}