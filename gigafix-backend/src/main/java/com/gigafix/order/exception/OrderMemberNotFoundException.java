package com.gigafix.order.exception;

/**
 * Order Service 無法依會員識別碼找到會員時拋出，
 * 再由模組的 ExceptionHandler 轉為 404 回應。
 */
public class OrderMemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OrderMemberNotFoundException(Long memberId) {
		super("找不到會員，memberId：" + memberId);
	}
}
