package com.gigafix.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CheckoutNotAvailableException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.cart.service.CartService;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.order.service.OrderService;
import com.gigafix.user.entity.Member;
import com.gigafix.user.repository.MemberRepository;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutTransactionIntegrationTest {

	@Autowired
	private OrderService orderService;
	@Autowired
	private CartService cartService;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CartRepository cartRepository;
	@Autowired
	private CartItemRepository cartItemRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;

	private Member member;
	private Cart cart;

	@BeforeEach
	void setUp() {
		orderItemRepository.deleteAll();
		orderRepository.deleteAll();
		cartItemRepository.deleteAll();
		cartRepository.deleteAll();
		memberRepository.deleteAll();

		member = memberRepository.save(Member.builder()
				.password("password")
				.realName("測試會員")
				.nickName("測試")
				.email("checkout@example.com")
				.phone("0900000000")
				.address("測試地址")
				.gender(Member.Gender.MALE)
				.createDateTime(LocalDateTime.now())
				.build());
		cart = cartRepository.save(Cart.builder()
				.member(member)
				.status(Cart.CartStatus.ACTIVE)
				.build());
		cartItemRepository.save(CartItem.builder()
				.cart(cart)
				.productId(10L)
				.quantity(2)
				.build());
	}

	@Test
	void checkoutDoesNotPersistIncompleteOrder() {
		assertThrows(
				CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(member.getId())
		);

		assertNoPartialCheckout();
	}

	@Test
	void sequentialCheckoutAttemptsAreBothRejectedWithoutDuplicateOrder() {
		assertThrows(
				CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(member.getId())
		);
		assertThrows(
				CheckoutNotAvailableException.class,
				() -> orderService.checkoutCart(member.getId())
		);

		assertNoPartialCheckout();
	}

	@Test
	void concurrentCheckoutAttemptsAreSerializedAndDoNotCreateOrders()
			throws Exception {
		var executor = Executors.newFixedThreadPool(2);
		var ready = new CountDownLatch(2);
		var start = new CountDownLatch(1);

		try {
			var first = executor.submit(() -> attemptCheckout(ready, start));
			var second = executor.submit(() -> attemptCheckout(ready, start));

			ready.await(5, TimeUnit.SECONDS);
			start.countDown();

			assertEquals(
					CheckoutNotAvailableException.class,
					first.get(10, TimeUnit.SECONDS)
			);
			assertEquals(
					CheckoutNotAvailableException.class,
					second.get(10, TimeUnit.SECONDS)
			);
		} finally {
			executor.shutdownNow();
		}

		assertNoPartialCheckout();
	}

	@Test
	void checkedOutHistoryDoesNotPreventCreatingNewActiveCart() {
		cart.setStatus(Cart.CartStatus.CHECKED_OUT);
		cartRepository.saveAndFlush(cart);

		cartService.addCartItem(
				member.getId(),
				new AddCartItemRequest(20L, 1)
		);

		Cart activeCart = cartRepository.findByMemberIdAndStatus(
				member.getId(),
				Cart.CartStatus.ACTIVE
		).orElseThrow();
		assertNotEquals(cart.getCartId(), activeCart.getCartId());
		assertEquals(2, cartRepository.count());
	}

	private Class<? extends RuntimeException> attemptCheckout(
			CountDownLatch ready,
			CountDownLatch start
	) throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			orderService.checkoutCart(member.getId());
			throw new AssertionError("checkout 不應在商品模組完成前成功");
		} catch (CheckoutNotAvailableException exception) {
			return exception.getClass();
		}
	}

	private void assertNoPartialCheckout() {
		assertEquals(0, orderRepository.count());
		assertEquals(0, orderItemRepository.count());
		assertEquals(
				Cart.CartStatus.ACTIVE,
				cartRepository.findById(cart.getCartId())
						.orElseThrow()
						.getStatus()
		);
	}
}
