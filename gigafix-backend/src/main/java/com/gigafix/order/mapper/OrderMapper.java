package com.gigafix.order.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gigafix.order.dto.response.OrderItemResponse;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;

/** 將訂單 entity 轉為 API response，不負責查詢或金額計算。 */
@Component
public class OrderMapper {

	public OrderItemResponse toItemResponse(OrderItem item) {
		return new OrderItemResponse(
				item.getOrderItemId(),
				item.getProductId(),
				item.getProductName(),
				item.getUnitPrice(),
				item.getCreatedAt(),
				item.getUpdatedAt()
		);
	}

	public OrderResponse toResponse(Order order, List<OrderItem> items) {
		List<OrderItem> safeItems = items == null ? List.of() : items;
		return new OrderResponse(
				order.getOrderId(),
				order.getMember().getId(),
				order.getOrderType(),
				order.getOrderDate(),
				order.getTotalAmount(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getRemark(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				safeItems.stream().map(this::toItemResponse).toList()
		);
	}
}
