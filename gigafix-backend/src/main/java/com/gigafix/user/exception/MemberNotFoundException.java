package com.gigafix.user.exception;

public class MemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MemberNotFoundException(Long memberId) {
		super("找不到會員，memberId：" + memberId);
	}
}
