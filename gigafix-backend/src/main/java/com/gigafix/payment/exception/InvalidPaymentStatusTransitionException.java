package com.gigafix.payment.exception;

import com.gigafix.payment.enums.PaymentRecordStatus;

/** 付款紀錄嘗試進入不合法狀態時拋出。 */
public class InvalidPaymentStatusTransitionException extends RuntimeException {
	public InvalidPaymentStatusTransitionException(PaymentRecordStatus from, PaymentRecordStatus to) {
		super("不允許將付款狀態由 " + from + " 變更為 " + to);
	}
}
