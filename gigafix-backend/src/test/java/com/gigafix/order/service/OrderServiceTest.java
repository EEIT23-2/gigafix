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
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.exception.OrderMemberNotFoundException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.user.entity.Member;
import com.gigafix.user.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderItemRepository orderItemRepository;
	@Mock
	private CartRepository cartRepository;
	@Mock
	private CartItemRepository cartItemRepository;
	@Mock
	private MemberRepository memberRepository;

	private OrderService orderService;
	private Member member;
	private Cart cart;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(
				orderRepository,
				orderItemRepository,
				cartRepository,
				cartItemRepository,
				memberRepository
		);
		member = Member.builder()
				.id(1L)
				.realName("會員一")
				.phone("0912345678")
				.address("台北市")
				.build();
		cart = Cart.builder()
				.cartId(100L)
				.member(member)
				.status(Cart.CartStatus.ACTIVE)
				.build();
	}

	@Test
	void checkout_requiresExistingMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(
				OrderMemberNotFoundException.class,
				() -> orderService.checkoutCart(99L)
		);
	}

	@Test
	void checkout_rejectsMissingOrAlreadyCheckedOutCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findForCheckoutByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());

		assertThrows(
				CartNotFoundException.class,
				() -> orderService.checkoutCart(1L)
		);
	}

	@Test
	void checkout_rejectsEmptyCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findForCheckoutByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartId(100L))
				.thenReturn(List.of());

		assertThrows(
				EmptyCartException.class,
				() -> orderService.checkoutCart(1L)
		);
	}

	@Test
	void checkoutRejectsIncompleteOrderUntilProductIntegrationExists() {
		CartItem cartItem = CartItem.builder()
				.cartItemId(200L)
				.cart(cart)
				.productId(10L)
				.quantity(2)
				.build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findForCheckoutByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartId(100L))
				.thenReturn(List.of(cartItem));
		assertThrows(
				CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(1L)
		);

		assertEquals(Cart.CartStatus.ACTIVE, cart.getStatus());
		verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(orderItemRepository, never()).saveAll(
				org.mockito.ArgumentMatchers.anyList()
		);
		verify(cartRepository, never()).save(cart);
	}

	@Test
	void checkoutUsesPessimisticLockQuery() {
		CartItem cartItem = CartItem.builder()
				.cart(cart)
				.productId(10L)
				.quantity(1)
				.build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findForCheckoutByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartId(100L))
				.thenReturn(List.of(cartItem));
		assertThrows(
				CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(1L)
		);
		verify(cartRepository).findForCheckoutByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		);
	}

	@Test
	void getOrderRejectsAnotherMembersOrder() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(
				OrderNotFoundException.class,
				() -> orderService.getOrder(1L, 300L)
		);
	}

	@Test
	void updateStatusRejectsAnotherMembersOrder() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(
				OrderNotFoundException.class,
				() -> orderService.updateOrderStatus(
						1L,
						300L,
						new UpdateOrderStatusRequest(
								Order.OrderStatus.PROCESSING
						)
				)
		);
	}

	@Test
	void getOrderReturnsOwnedOrder() {
		Order order = ownedOrder();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.of(order));
		when(orderItemRepository.findByOrderOrderId(300L))
				.thenReturn(List.of());

		var response = orderService.getOrder(1L, 300L);

		assertEquals(1L, response.memberId());
	}

	@Test
	void getOrdersReturnsOnlyMembersOrders() {
		Order order = ownedOrder();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByMemberIdOrderByCreatedAtDesc(1L))
				.thenReturn(List.of(order));
		when(orderItemRepository.findByOrderOrderId(300L))
				.thenReturn(List.of());

		assertEquals(1, orderService.getOrdersByMember(1L).size());
	}

	@Test
	void updateStatusUpdatesOwnedOrder() {
		Order order = ownedOrder();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.of(order));
		when(orderRepository.save(order)).thenReturn(order);
		when(orderItemRepository.findByOrderOrderId(300L))
				.thenReturn(List.of());

		orderService.updateOrderStatus(
				1L,
				300L,
				new UpdateOrderStatusRequest(Order.OrderStatus.PROCESSING)
		);

		assertEquals(Order.OrderStatus.PROCESSING, order.getStatus());
	}

	@Test
	void updatePaymentUpdatesOwnedOrder() {
		Order order = ownedOrder();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.of(order));
		when(orderRepository.save(order)).thenReturn(order);
		when(orderItemRepository.findByOrderOrderId(300L))
				.thenReturn(List.of());

		orderService.updatePaymentStatus(
				1L,
				300L,
				new UpdatePaymentStatusRequest(Order.PaymentStatus.PAID)
		);

		assertEquals(Order.PaymentStatus.PAID, order.getPaymentStatus());
	}

	@Test
	void deleteOrderDeletesItemsBeforeOwnedOrder() {
		Order order = ownedOrder();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(orderRepository.findByOrderIdAndMemberId(300L, 1L))
				.thenReturn(Optional.of(order));

		orderService.deleteOrder(1L, 300L);

		verify(orderItemRepository).deleteByOrderOrderId(300L);
		verify(orderRepository).delete(order);
	}

	private Order ownedOrder() {
		return Order.builder()
				.orderId(300L)
				.member(member)
				.receiverName("會員一")
				.receiverPhone("0912345678")
				.shippingAddress("台北市")
				.status(Order.OrderStatus.PENDING)
				.paymentStatus(Order.PaymentStatus.UNPAID)
				.build();
	}
}
