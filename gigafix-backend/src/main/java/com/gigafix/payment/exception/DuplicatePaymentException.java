package com.gigafix.payment.exception;

/** 同一張訂單重複建立付款紀錄時拋出。 */
public class DuplicatePaymentException extends RuntimeException {
	public DuplicatePaymentException(Long orderId) { super("訂單已有付款紀錄，orderId：" + orderId); }
}
