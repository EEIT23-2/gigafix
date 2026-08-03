package com.gigafix.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.gigafix.payment.enums.PaymentMethod;
import com.gigafix.payment.enums.PaymentRecordStatus;
import com.gigafix.order.enums.OrderType;

/** 提供付款紀錄的安全回應，不直接暴露關聯的訂單 Entity。 */
public record PaymentResponse(
		Long paymentId,
		Long orderId,
		OrderType orderType,
		PaymentMethod paymentMethod,
		PaymentRecordStatus paymentStatus,
		String transactionId,
		BigDecimal amount,
		LocalDateTime paidAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
