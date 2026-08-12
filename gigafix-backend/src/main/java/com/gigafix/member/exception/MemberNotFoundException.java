package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends MemberException {
	public MemberNotFoundException() {
		//直接在子類別建構式寫死資訊，不用每次throw這個錯誤的時候寫1次
		super("MEMBER_NOT_FOUND", "使用者不存在", HttpStatus.UNAUTHORIZED); //401
	}
}
