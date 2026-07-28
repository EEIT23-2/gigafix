package com.gigafix.order.dto.request;

import com.gigafix.order.entity.Order;

import jakarta.validation.constraints.NotNull;

/**
 * 前端修改訂單處理進度時傳入的請求 DTO。
 * {@code status} 由 Controller 驗證後交給 Order Service 更新 Order Entity。
 */
public record UpdateOrderStatusRequest(
		@NotNull
		Order.OrderStatus status
) {
}
