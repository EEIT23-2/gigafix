package com.gigafix.order.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.gigafix.order.entity.Order;
import com.gigafix.order.exception.InvalidOrderStatusTransitionException;

/** 驗證訂單狀態政策只接受正式允許的轉換。 */
class OrderStatusPolicyTest {

	private final OrderStatusPolicy policy = new OrderStatusPolicy();

	@Test
	void allowsDefinedForwardAndCancellationTransitions() {
		assertAllowed(Order.OrderStatus.PENDING, Order.OrderStatus.PROCESSING);
		assertAllowed(Order.OrderStatus.PENDING, Order.OrderStatus.CANCELLED);
		assertAllowed(Order.OrderStatus.PROCESSING, Order.OrderStatus.SHIPPED);
		assertAllowed(Order.OrderStatus.PROCESSING, Order.OrderStatus.CANCELLED);
		assertAllowed(Order.OrderStatus.SHIPPED, Order.OrderStatus.COMPLETED);
	}

	@Test
	void rejectsSkippedBackwardAndTerminalTransitions() {
		assertRejected(Order.OrderStatus.PENDING, Order.OrderStatus.COMPLETED);
		assertRejected(Order.OrderStatus.SHIPPED, Order.OrderStatus.PROCESSING);
		assertRejected(Order.OrderStatus.COMPLETED, Order.OrderStatus.PENDING);
		assertRejected(Order.OrderStatus.CANCELLED, Order.OrderStatus.SHIPPED);
		assertRejected(Order.OrderStatus.CANCELLED, Order.OrderStatus.CANCELLED);
		assertRejected(Order.OrderStatus.COMPLETED, Order.OrderStatus.COMPLETED);
		assertRejected(Order.OrderStatus.SHIPPED, Order.OrderStatus.CANCELLED);
	}

	private void assertAllowed(Order.OrderStatus current, Order.OrderStatus target) {
		assertDoesNotThrow(() -> policy.validateTransition(current, target));
	}

	private void assertRejected(Order.OrderStatus current, Order.OrderStatus target) {
		assertThrows(InvalidOrderStatusTransitionException.class,
				() -> policy.validateTransition(current, target));
	}
}
