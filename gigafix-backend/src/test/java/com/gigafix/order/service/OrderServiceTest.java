package com.gigafix.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gigafix.order.dto.request.CreateOrderItemRequest;
import com.gigafix.order.dto.request.CreateOrderRequest;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.exception.InvalidOrderException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private static final LocalDateTime FIXED_TIME =
			LocalDateTime.of(2026, 7, 27, 10, 0);

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@InjectMocks
	private OrderService orderService;

	@Test
	@DisplayName("成功建立訂單與明細")
	void createOrder_createsOrderAndItemsSuccessfully() {
		CreateOrderRequest request = createRequest(
				new BigDecimal("20.00"),
				new BigDecimal("10.00"),
				List.of(
						new CreateOrderItemRequest(
								10L,
								"商品 A",
								new BigDecimal("100.00"),
								2
						),
						new CreateOrderItemRequest(
								20L,
								"商品 B",
								new BigDecimal("50.00"),
								1
						)
				)
		);
		when(orderRepository.save(any(Order.class)))
				.thenAnswer(invocation -> {
					Order order = invocation.getArgument(0);
					order.setOrderId(100L);
					order.setStatus(Order.OrderStatus.PENDING);
					order.setPaymentStatus(Order.PaymentStatus.UNPAID);
					order.setOrderDate(FIXED_TIME);
					order.setCreatedAt(FIXED_TIME);
					order.setUpdatedAt(FIXED_TIME);
					return order;
				});
		when(orderItemRepository.saveAll(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		OrderResponse response = orderService.createOrder(request);

		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository, times(1)).save(orderCaptor.capture());
		assertEquals(
				new BigDecimal("260.00"),
				orderCaptor.getValue().getTotalAmount()
		);

		ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.captor();
		verify(orderItemRepository, times(1)).saveAll(itemsCaptor.capture());
		List<OrderItem> savedItems = itemsCaptor.getValue();
		assertEquals(2, savedItems.size());
		assertTrue(savedItems.stream()
				.allMatch(item -> item.getOrderId().equals(100L)));
		assertEquals(
				new BigDecimal("200.00"),
				savedItems.get(0).getSubtotal()
		);
		assertEquals(
				new BigDecimal("50.00"),
				savedItems.get(1).getSubtotal()
		);
		assertEquals(100L, response.orderId());
		assertEquals(new BigDecimal("260.00"), response.totalAmount());
		assertEquals(2, response.items().size());
	}

	@Test
	@DisplayName("沒有明細時拋出 InvalidOrderException")
	void createOrder_throwsExceptionWhenItemsAreEmpty() {
		CreateOrderRequest request = createRequest(
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				List.of()
		);

		assertThrows(
				InvalidOrderException.class,
				() -> orderService.createOrder(request)
		);
		verifyNoInteractions(orderRepository, orderItemRepository);
	}

	@Test
	@DisplayName("商品數量小於等於零時拋出 InvalidOrderException")
	void createOrder_throwsExceptionWhenQuantityIsNotPositive() {
		CreateOrderRequest request = createRequest(
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				List.of(new CreateOrderItemRequest(
						10L,
						"商品 A",
						new BigDecimal("100.00"),
						0
				))
		);

		assertThrows(
				InvalidOrderException.class,
				() -> orderService.createOrder(request)
		);
		verifyNoInteractions(orderRepository, orderItemRepository);
	}

	@Test
	@DisplayName("訂單總金額小於零時拋出 InvalidOrderException")
	void createOrder_throwsExceptionWhenTotalAmountIsNegative() {
		CreateOrderRequest request = createRequest(
				BigDecimal.ZERO,
				new BigDecimal("20.00"),
				List.of(new CreateOrderItemRequest(
						10L,
						"商品 A",
						new BigDecimal("10.00"),
						1
				))
		);

		assertThrows(
				InvalidOrderException.class,
				() -> orderService.createOrder(request)
		);
		verifyNoInteractions(orderRepository, orderItemRepository);
	}

	@Test
	@DisplayName("成功取得訂單")
	void getOrder_returnsOrderSuccessfully() {
		Order order = createOrder(1L, 1L);
		List<OrderItem> items = List.of(createOrderItem(1L, 1L));
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderItemRepository.findByOrderId(1L)).thenReturn(items);

		OrderResponse response = orderService.getOrder(1L);

		assertEquals(1L, response.orderId());
		assertEquals(1L, response.userId());
		assertEquals(1, response.items().size());
		verify(orderRepository, times(1)).findById(1L);
		verify(orderItemRepository, times(1)).findByOrderId(1L);
	}

	@Test
	@DisplayName("取得訂單時找不到則拋出 OrderNotFoundException")
	void getOrder_throwsExceptionWhenOrderDoesNotExist() {
		when(orderRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(
				OrderNotFoundException.class,
				() -> orderService.getOrder(1L)
		);
		verifyNoInteractions(orderItemRepository);
	}

	@Test
	@DisplayName("成功回傳會員的多筆訂單")
	void getOrdersByUser_returnsMultipleOrdersSuccessfully() {
		Order firstOrder = createOrder(1L, 1L);
		Order secondOrder = createOrder(2L, 1L);
		when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
				.thenReturn(List.of(firstOrder, secondOrder));
		when(orderItemRepository.findByOrderId(1L))
				.thenReturn(List.of(createOrderItem(1L, 1L)));
		when(orderItemRepository.findByOrderId(2L))
				.thenReturn(List.of(createOrderItem(2L, 2L)));

		List<OrderResponse> responses = orderService.getOrdersByUser(1L);

		assertEquals(2, responses.size());
		assertTrue(responses.stream()
				.allMatch(response -> response.userId().equals(1L)));
		verify(orderRepository, times(1))
				.findByUserIdOrderByCreatedAtDesc(1L);
		verify(orderItemRepository, times(1)).findByOrderId(1L);
		verify(orderItemRepository, times(1)).findByOrderId(2L);
	}

	@Test
	@DisplayName("成功更新訂單狀態")
	void updateOrderStatus_updatesStatusSuccessfully() {
		Order order = createOrder(1L, 1L);
		UpdateOrderStatusRequest request =
				new UpdateOrderStatusRequest(Order.OrderStatus.SHIPPED);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderRepository.save(order)).thenReturn(order);
		when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

		OrderResponse response = orderService.updateOrderStatus(1L, request);

		assertEquals(Order.OrderStatus.SHIPPED, order.getStatus());
		assertEquals(Order.OrderStatus.SHIPPED, response.status());
		verify(orderRepository, times(1)).save(order);
	}

	@Test
	@DisplayName("成功更新付款狀態")
	void updatePaymentStatus_updatesPaymentStatusSuccessfully() {
		Order order = createOrder(1L, 1L);
		UpdatePaymentStatusRequest request =
				new UpdatePaymentStatusRequest(Order.PaymentStatus.PAID);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderRepository.save(order)).thenReturn(order);
		when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

		OrderResponse response =
				orderService.updatePaymentStatus(1L, request);

		assertEquals(Order.PaymentStatus.PAID, order.getPaymentStatus());
		assertEquals(Order.PaymentStatus.PAID, response.paymentStatus());
		verify(orderRepository, times(1)).save(order);
	}

	@Test
	@DisplayName("成功刪除訂單且先刪明細再刪主表")
	void deleteOrder_deletesItemsBeforeOrder() {
		Order order = createOrder(1L, 1L);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

		orderService.deleteOrder(1L);

		InOrder deletionOrder = inOrder(
				orderItemRepository,
				orderRepository
		);
		deletionOrder.verify(orderItemRepository).deleteByOrderId(1L);
		deletionOrder.verify(orderRepository).delete(order);
	}

	@Test
	@DisplayName("刪除訂單時找不到則拋出 OrderNotFoundException")
	void deleteOrder_throwsExceptionWhenOrderDoesNotExist() {
		when(orderRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(
				OrderNotFoundException.class,
				() -> orderService.deleteOrder(1L)
		);
		verify(orderItemRepository, never()).deleteByOrderId(1L);
		verify(orderRepository, never()).delete(any(Order.class));
	}

	private CreateOrderRequest createRequest(
			BigDecimal shippingFee,
			BigDecimal discountAmount,
			List<CreateOrderItemRequest> items
	) {
		return new CreateOrderRequest(
				1L,
				"王小明",
				"0912345678",
				"台北市中正區測試路 1 號",
				shippingFee,
				discountAmount,
				"測試訂單",
				items
		);
	}

	private Order createOrder(Long orderId, Long userId) {
		return Order.builder()
				.orderId(orderId)
				.userId(userId)
				.orderDate(FIXED_TIME)
				.totalAmount(new BigDecimal("100.00"))
				.status(Order.OrderStatus.PENDING)
				.paymentStatus(Order.PaymentStatus.UNPAID)
				.receiverName("王小明")
				.receiverPhone("0912345678")
				.shippingAddress("台北市中正區測試路 1 號")
				.shippingFee(BigDecimal.ZERO)
				.discountAmount(BigDecimal.ZERO)
				.remark("測試訂單")
				.createdAt(FIXED_TIME)
				.updatedAt(FIXED_TIME)
				.build();
	}

	private OrderItem createOrderItem(Long orderItemId, Long orderId) {
		return OrderItem.builder()
				.orderItemId(orderItemId)
				.orderId(orderId)
				.productId(10L)
				.productName("商品 A")
				.unitPrice(new BigDecimal("100.00"))
				.quantity(1)
				.subtotal(new BigDecimal("100.00"))
				.createdAt(FIXED_TIME)
				.updatedAt(FIXED_TIME)
				.build();
	}
}
