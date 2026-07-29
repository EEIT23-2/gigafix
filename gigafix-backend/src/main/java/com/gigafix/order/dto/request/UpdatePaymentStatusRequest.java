package com.gigafix.order.dto.request;

import com.gigafix.order.entity.Order;

import jakarta.validation.constraints.NotNull;

/**
 * 前端修改訂單付款結果時傳入的請求 DTO。
 * {@code paymentStatus} 由 Controller 驗證後交給 Order Service 更新 Order Entity。
 */
public record UpdatePaymentStatusRequest(
		@NotNull
		Order.PaymentStatus paymentStatus
) {
}
