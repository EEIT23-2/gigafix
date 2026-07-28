package com.gigafix.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.gigafix.order.entity.Order;

public record OrderResponse(
		Long orderId,
		Long memberId,
		LocalDateTime orderDate,
		BigDecimal totalAmount,
		Order.OrderStatus status,
		Order.PaymentStatus paymentStatus,
		String receiverName,
		String receiverPhone,
		String shippingAddress,
		BigDecimal shippingFee,
		BigDecimal discountAmount,
		String remark,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<OrderItemResponse> items
) {
}
