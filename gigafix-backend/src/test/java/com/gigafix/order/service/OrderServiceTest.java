package com.gigafix.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.CheckoutNotAvailableException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.exception.InvalidOrderStatusTransitionException;
import com.gigafix.order.exception.OrderMemberNotFoundException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.enums.OrderType;
import com.gigafix.order.mapper.OrderMapper;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;

/** 驗證訂單查詢、取消與狀態政策的服務流程。 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock OrderRepository orderRepository;
	@Mock OrderItemRepository orderItemRepository;
	@Mock CartRepository cartRepository;
	@Mock CartItemRepository cartItemRepository;
	@Mock MemberRepository memberRepository;

	private OrderService orderService;
	private Member member;
	private Cart cart;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(
				orderRepository, orderItemRepository, cartRepository,
				cartItemRepository, memberRepository,
				new OrderMapper(), new OrderStatusPolicy()
		);
		member = Member.builder().id(1L).build();
		cart = Cart.builder().cartId(100L).member(member)
				.status(Cart.CartStatus.ACTIVE).build();
	}

	@Test
	void checkoutRequiresExistingMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(OrderMemberNotFoundException.class,
				() -> orderService.checkoutCart(99L));
	}

	@Test
	void checkoutRejectsMissingActiveCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findForCheckoutByMemberIdAndStatus(
				1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.empty());
		assertThrows(CartNotFoundException.class,
				() -> orderService.checkoutCart(1L));
	}

	@Test
	void checkoutRejectsEmptyCart() {
		stubActiveCart();
		when(cartItemRepository.findByCartCartId(100L)).thenReturn(List.of());
		assertThrows(EmptyCartException.class,
				() -> orderService.checkoutCart(1L));
	}

	@Test
	void checkoutRemainsBlockedUntilProductIntegrationExists() {
		stubCheckoutItem();
		assertThrows(CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(1L));
		assertEquals(Cart.CartStatus.ACTIVE, cart.getStatus());
		verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(orderItemRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
		verify(cartRepository, never()).save(cart);
	}

	@Test
	void checkoutUsesPessimisticLockQuery() {
		stubCheckoutItem();
		assertThrows(CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(1L));
		verify(cartRepository).findForCheckoutByMemberIdAndStatus(
				1L, Cart.CartStatus.ACTIVE);
	}

	@Test
	void getOrderRejectsAnotherMembersOrder() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.empty());
		assertThrows(OrderNotFoundException.class,
				() -> orderService.getOrder(1L, 300L));
	}

	@Test
	void getOrderReturnsSnapshotItems() {
		Order order = ownedOrder(300L);
		OrderItem item = item(order, 10L);
		stubOwnedOrder(order);
		when(orderItemRepository.findByOrderOrderId(300L)).thenReturn(List.of(item));
		var response = orderService.getOrder(1L, 300L);
		assertEquals(1L, response.memberId());
		assertEquals("iPhone snapshot", response.items().getFirst().productName());
	}

	@Test
	void getOrdersBulkLoadsItemsOnce() {
		Order first = ownedOrder(300L);
		Order second = ownedOrder(301L);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByMemberIdOrderByCreatedAtDesc(1L))
				.thenReturn(List.of(first, second));
		when(orderItemRepository.findAllByOrderOrderIdIn(List.of(300L, 301L)))
				.thenReturn(List.of(item(first, 10L), item(second, 11L)));
		assertEquals(2, orderService.getOrdersByMember(1L).size());
		verify(orderItemRepository).findAllByOrderOrderIdIn(List.of(300L, 301L));
		verify(orderItemRepository, never()).findByOrderOrderId(300L);
		verify(orderItemRepository, never()).findByOrderOrderId(301L);
	}

	@Test
	void emptyOrderListDoesNotQueryItems() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByMemberIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
		assertEquals(List.of(), orderService.getOrdersByMember(1L));
		verify(orderItemRepository, never())
				.findAllByOrderOrderIdIn(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void updateStatusUsesPolicy() {
		Order order = ownedOrder(300L);
		stubOwnedOrder(order);
		when(orderRepository.save(order)).thenReturn(order);
		when(orderItemRepository.findByOrderOrderId(300L)).thenReturn(List.of());
		orderService.updateOrderStatus(1L, 300L,
				new UpdateOrderStatusRequest(Order.OrderStatus.PROCESSING));
		assertEquals(Order.OrderStatus.PROCESSING, order.getStatus());
	}

	@Test
	void updateStatusRejectsIllegalTransitionBeforeSave() {
		Order order = ownedOrder(300L);
		stubOwnedOrder(order);
		assertThrows(InvalidOrderStatusTransitionException.class,
				() -> orderService.updateOrderStatus(1L, 300L,
						new UpdateOrderStatusRequest(Order.OrderStatus.COMPLETED)));
		verify(orderRepository, never()).save(order);
	}

	@Test
	void cancelOrderChangesStatusWithoutDeletingSnapshots() {
		Order order = ownedOrder(300L);
		stubOwnedOrder(order);
		when(orderRepository.save(order)).thenReturn(order);
		when(orderItemRepository.findByOrderOrderId(300L)).thenReturn(List.of());
		orderService.cancelOrder(1L, 300L);
		assertEquals(Order.OrderStatus.CANCELLED, order.getStatus());
		verify(orderItemRepository, never()).deleteByOrderOrderId(300L);
		verify(orderRepository, never()).delete(order);
	}

	private void stubActiveCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findForCheckoutByMemberIdAndStatus(
				1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
	}

	private void stubCheckoutItem() {
		stubActiveCart();
		when(cartItemRepository.findByCartCartId(100L)).thenReturn(List.of(
				CartItem.builder().cart(cart).productId(10L).build()
		));
	}

	private void stubOwnedOrder(Order order) {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(order.getOrderId(), 1L))
				.thenReturn(Optional.of(order));
	}

	private Order ownedOrder(Long id) {
		return Order.builder().orderId(id).member(member)
				.orderType(OrderType.GENERAL)
				.status(Order.OrderStatus.PENDING)
				.paymentStatus(Order.PaymentStatus.UNPAID).build();
	}

	private OrderItem item(Order order, Long productId) {
		return OrderItem.builder().order(order).productId(productId)
				.productName("iPhone snapshot").unitPrice(java.math.BigDecimal.TEN).build();
	}
}
