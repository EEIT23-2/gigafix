package com.gigafix.order.dto.request;

import com.gigafix.order.entity.Order;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
		@NotNull
		Order.OrderStatus status
) {
}
