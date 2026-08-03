package com.gigafix.payment.mapper;

import org.springframework.stereotype.Component;

import com.gigafix.payment.dto.response.PaymentResponse;
import com.gigafix.payment.entity.Payment;

@Component
/** 將 Payment Entity 轉為回應 DTO，不負責查詢或業務判斷。 */
public class PaymentMapper {
	/** 只輸出訂單識別碼，避免直接回傳延遲載入的關聯 Entity。 */
	public PaymentResponse toResponse(Payment payment) {
		return new PaymentResponse(
				payment.getPaymentId(), payment.getOrder().getOrderId(),
				payment.getOrder().getOrderType(),
				payment.getPaymentMethod(), payment.getPaymentStatus(),
				payment.getTransactionId(), payment.getAmount(), payment.getPaidAt(),
				payment.getCreatedAt(), payment.getUpdatedAt()
		);
	}
}
