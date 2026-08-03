package com.gigafix.payment.exception;

/** 找不到會員可存取的訂單或付款紀錄時拋出。 */
public class PaymentNotFoundException extends RuntimeException {
	public PaymentNotFoundException(String message) { super(message); }
}
