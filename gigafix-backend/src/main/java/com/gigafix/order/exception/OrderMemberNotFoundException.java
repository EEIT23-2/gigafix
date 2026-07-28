package com.gigafix.order.exception;

public class OrderMemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OrderMemberNotFoundException(Long memberId) {
		super("找不到會員，memberId：" + memberId);
	}
}
