package com.gigafix.order.exception;

import com.gigafix.order.entity.Order;

/** 訂單狀態不允許依指定方向轉換時拋出。 */
public class InvalidOrderStatusTransitionException extends RuntimeException {

	public InvalidOrderStatusTransitionException(
			Order.OrderStatus current,
			Order.OrderStatus target
	) {
		super("不允許將訂單狀態由 " + current + " 變更為 " + target);
	}
}
