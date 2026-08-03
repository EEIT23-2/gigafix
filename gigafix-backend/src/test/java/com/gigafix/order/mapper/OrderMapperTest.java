package com.gigafix.order.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gigafix.member.entity.Member;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.enums.OrderType;

/** 驗證訂單與商品快照能正確映射為回應 DTO。 */
class OrderMapperTest {

	private final OrderMapper mapper = new OrderMapper();

	@Test
	void mapsOrderAndSnapshotItemWithoutChangingMoney() {
		Member member = Member.builder().id(1L).build();
		Order order = Order.builder().orderId(10L).member(member)
				.orderType(OrderType.GENERAL)
				.totalAmount(new BigDecimal("1234.50"))
				.status(Order.OrderStatus.PENDING)
				.paymentStatus(Order.PaymentStatus.UNPAID).build();
		OrderItem item = OrderItem.builder().order(order).productId(20L)
				.productName("snapshot").unitPrice(new BigDecimal("1234.50")).build();

		var response = mapper.toResponse(order, List.of(item));

		assertEquals(new BigDecimal("1234.50"), response.totalAmount());
		assertEquals("snapshot", response.items().getFirst().productName());
		assertEquals(OrderType.GENERAL, response.orderType());
		assertEquals(new BigDecimal("1234.50"), response.items().getFirst().unitPrice());
	}

	@Test
	void mapsNullItemsAsEmptyList() {
		Order order = Order.builder().member(Member.builder().id(1L).build()).orderType(OrderType.REPAIR).build();
		assertEquals(List.of(), mapper.toResponse(order, null).items());
	}
}
