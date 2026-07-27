package com.gigafix.order.dto.request;

import com.gigafix.order.entity.Order;

import jakarta.validation.constraints.NotNull;

public record UpdatePaymentStatusRequest(
		@NotNull
		Order.PaymentStatus paymentStatus
) {
}
