package com.gigafix.payment.exception;

/** 訂單或付款狀態不允許目前操作時拋出。 */
public class InvalidPaymentOperationException extends RuntimeException {
	public InvalidPaymentOperationException(String message) { super(message); }
}
