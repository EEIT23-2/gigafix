package com.gigafix.order.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.gigafix.order.entity.Order;
import com.gigafix.order.exception.InvalidOrderStatusTransitionException;

/** 集中管理訂單狀態轉換規則。 */
@Component
public class OrderStatusPolicy {

	private static final Map<Order.OrderStatus, Set<Order.OrderStatus>> ALLOWED =
			new EnumMap<>(Order.OrderStatus.class);

	static {
		ALLOWED.put(Order.OrderStatus.PENDING, EnumSet.of(
				Order.OrderStatus.PROCESSING,
				Order.OrderStatus.CANCELLED
		));
		ALLOWED.put(Order.OrderStatus.PROCESSING, EnumSet.of(
				Order.OrderStatus.SHIPPED,
				Order.OrderStatus.CANCELLED
		));
		ALLOWED.put(Order.OrderStatus.SHIPPED, EnumSet.of(
				Order.OrderStatus.COMPLETED
		));
		ALLOWED.put(Order.OrderStatus.COMPLETED, EnumSet.noneOf(Order.OrderStatus.class));
		ALLOWED.put(Order.OrderStatus.CANCELLED, EnumSet.noneOf(Order.OrderStatus.class));
	}

	/** 驗證訂單狀態轉換，非法方向會拋出明確例外。 */
	public void validateTransition(
			Order.OrderStatus current,
			Order.OrderStatus target
	) {
		if (current == null || target == null
				|| !ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
			throw new InvalidOrderStatusTransitionException(current, target);
		}
	}
}
