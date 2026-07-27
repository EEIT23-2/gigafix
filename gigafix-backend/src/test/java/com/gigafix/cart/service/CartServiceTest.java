package com.gigafix.cart.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.exception.InvalidCartQuantityException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	private static final LocalDateTime FIXED_TIME =
			LocalDateTime.of(2026, 7, 1, 10, 0);

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@InjectMocks
	private CartService cartService;

	@Test
	@DisplayName("沒有 ACTIVE Cart 時建立 Cart 與 CartItem")
	void addCartItem_createsCartAndItemWhenActiveCartDoesNotExist() {
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());
		when(cartRepository.save(any(Cart.class)))
				.thenAnswer(invocation -> {
					Cart cart = invocation.getArgument(0);
					cart.setCartId(100L);
					return cart;
				});
		when(cartItemRepository.findByCartIdAndProductId(100L, 10L))
				.thenReturn(Optional.empty());
		when(cartItemRepository.save(any(CartItem.class)))
				.thenAnswer(invocation -> {
					CartItem item = invocation.getArgument(0);
					item.setCartItemId(1L);
					return item;
				});

		CartItemResponse response = cartService.addCartItem(request);

		ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
		verify(cartRepository, times(1)).save(cartCaptor.capture());
		assertEquals(1L, cartCaptor.getValue().getUserId());
		assertEquals(
				Cart.CartStatus.ACTIVE,
				cartCaptor.getValue().getStatus()
		);

		ArgumentCaptor<CartItem> itemCaptor =
				ArgumentCaptor.forClass(CartItem.class);
		verify(cartItemRepository, times(1)).save(itemCaptor.capture());
		CartItem savedItem = itemCaptor.getValue();
		assertEquals(100L, savedItem.getCartId());
		assertEquals(10L, savedItem.getProductId());
		assertEquals(2, savedItem.getQuantity());
		assertEquals(100L, response.cartId());
		assertEquals(10L, response.productId());
		assertEquals(2, response.quantity());
	}

	@Test
	@DisplayName("已有 CHECKED_OUT 歷史 Cart 時仍可建立新的 ACTIVE Cart")
	void addCartItem_createsActiveCartWhenCheckedOutHistoryExists() {
		Cart checkedOutCart = Cart.builder()
				.cartId(99L)
				.userId(1L)
				.status(Cart.CartStatus.CHECKED_OUT)
				.createdAt(FIXED_TIME)
				.updatedAt(FIXED_TIME)
				.build();
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());
		when(cartRepository.save(any(Cart.class)))
				.thenAnswer(invocation -> {
					Cart cart = invocation.getArgument(0);
					cart.setCartId(100L);
					return cart;
				});
		when(cartItemRepository.findByCartIdAndProductId(100L, 10L))
				.thenReturn(Optional.empty());
		when(cartItemRepository.save(any(CartItem.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CartItemResponse response = cartService.addCartItem(request);

		ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
		verify(cartRepository, times(1)).save(cartCaptor.capture());
		Cart newActiveCart = cartCaptor.getValue();
		assertEquals(Cart.CartStatus.CHECKED_OUT, checkedOutCart.getStatus());
		assertEquals(99L, checkedOutCart.getCartId());
		assertEquals(Cart.CartStatus.ACTIVE, newActiveCart.getStatus());
		assertEquals(100L, newActiveCart.getCartId());
		assertEquals(1L, newActiveCart.getUserId());
		assertEquals(100L, response.cartId());
		verify(cartRepository, never())
				.findByUserIdOrderByCreatedAtDesc(1L);
		verify(cartRepository, never()).existsByUserIdAndStatus(
				1L,
				Cart.CartStatus.CHECKED_OUT
		);
	}

	@Test
	@DisplayName("已有 ACTIVE Cart 時新增 CartItem")
	void addCartItem_addsItemToExistingActiveCart() {
		Cart cart = activeCart(100L, 1L);
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartIdAndProductId(100L, 10L))
				.thenReturn(Optional.empty());
		when(cartItemRepository.save(any(CartItem.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CartItemResponse response = cartService.addCartItem(request);

		ArgumentCaptor<CartItem> itemCaptor =
				ArgumentCaptor.forClass(CartItem.class);
		verify(cartItemRepository, times(1)).save(itemCaptor.capture());
		assertEquals(100L, itemCaptor.getValue().getCartId());
		assertEquals(10L, itemCaptor.getValue().getProductId());
		assertEquals(2, response.quantity());
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("商品已存在時累加數量")
	void addCartItem_increasesQuantityWhenProductExists() {
		Cart cart = activeCart(100L, 1L);
		CartItem existingItem = cartItem(1L, 100L, 10L, 3);
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartIdAndProductId(100L, 10L))
				.thenReturn(Optional.of(existingItem));
		when(cartItemRepository.save(existingItem))
				.thenReturn(existingItem);

		CartItemResponse response = cartService.addCartItem(request);

		ArgumentCaptor<CartItem> itemCaptor =
				ArgumentCaptor.forClass(CartItem.class);
		verify(cartItemRepository, times(1)).save(itemCaptor.capture());
		assertSame(existingItem, itemCaptor.getValue());
		assertEquals(5, existingItem.getQuantity());
		assertEquals(5, response.quantity());
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("已有購物車時回傳 Cart 與 Items")
	void getActiveCart_returnsExistingCartAndItems() {
		Cart cart = activeCart(100L, 1L);
		List<CartItem> items = List.of(
				cartItem(1L, 100L, 10L, 2),
				cartItem(2L, 100L, 20L, 3)
		);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartId(100L)).thenReturn(items);

		CartResponse response = cartService.getActiveCart(1L);

		assertEquals(100L, response.cartId());
		assertEquals(1L, response.userId());
		assertEquals(Cart.CartStatus.ACTIVE, response.status());
		assertEquals(2, response.items().size());
		assertTrue(response.items().stream()
				.allMatch(item -> item.cartId().equals(100L)));
		verify(cartItemRepository, times(1)).findByCartId(100L);
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("沒有 ACTIVE Cart 時拋出 CartNotFoundException")
	void getActiveCart_throwsExceptionWhenCartDoesNotExist() {
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());

		CartNotFoundException exception = assertThrows(
				CartNotFoundException.class,
				() -> cartService.getActiveCart(1L)
		);

		assertTrue(exception.getMessage().contains("userId：1"));
		verify(cartRepository, never()).save(any(Cart.class));
		verify(cartItemRepository, never()).findByCartId(any(Long.class));
	}

	@Test
	@DisplayName("userId 不合法時拋出例外")
	void getActiveCart_throwsExceptionWhenUserIdIsInvalid() {
		assertAll(
				() -> assertThrows(
						IllegalArgumentException.class,
						() -> cartService.getActiveCart(null)
				),
				() -> assertThrows(
						IllegalArgumentException.class,
						() -> cartService.getActiveCart(0L)
				),
				() -> assertThrows(
						IllegalArgumentException.class,
						() -> cartService.getActiveCart(-1L)
				)
		);
		verifyNoInteractions(cartRepository, cartItemRepository);
	}

	@Test
	@DisplayName("成功修改購物車數量")
	void updateQuantity_updatesQuantitySuccessfully() {
		CartItem existingItem = cartItem(1L, 100L, 10L, 2);
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		when(cartItemRepository.findById(1L))
				.thenReturn(Optional.of(existingItem));
		when(cartItemRepository.save(existingItem))
				.thenReturn(existingItem);

		CartItemResponse response = cartService.updateQuantity(1L, request);

		ArgumentCaptor<CartItem> itemCaptor =
				ArgumentCaptor.forClass(CartItem.class);
		verify(cartItemRepository, times(1)).save(itemCaptor.capture());
		assertSame(existingItem, itemCaptor.getValue());
		assertEquals(5, itemCaptor.getValue().getQuantity());
		assertEquals(1L, response.cartItemId());
		assertEquals(100L, response.cartId());
		assertEquals(5, response.quantity());
	}

	@Test
	@DisplayName("修改數量時找不到項目則拋出例外")
	void updateQuantity_throwsExceptionWhenItemDoesNotExist() {
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		when(cartItemRepository.findById(1L))
				.thenReturn(Optional.empty());

		assertThrows(
				CartItemNotFoundException.class,
				() -> cartService.updateQuantity(1L, request)
		);
		verify(cartItemRepository, never()).save(any(CartItem.class));
	}

	@Test
	@DisplayName("成功刪除購物車項目")
	void deleteCartItem_deletesItemSuccessfully() {
		CartItem existingItem = cartItem(1L, 100L, 10L, 2);
		when(cartItemRepository.findById(1L))
				.thenReturn(Optional.of(existingItem));

		cartService.deleteCartItem(1L);

		verify(cartItemRepository, times(1)).delete(existingItem);
	}

	@Test
	@DisplayName("刪除時找不到項目則拋出例外")
	void deleteCartItem_throwsExceptionWhenItemDoesNotExist() {
		when(cartItemRepository.findById(1L))
				.thenReturn(Optional.empty());

		assertThrows(
				CartItemNotFoundException.class,
				() -> cartService.deleteCartItem(1L)
		);
		verify(cartItemRepository, never()).delete(any(CartItem.class));
	}

	@Test
	@DisplayName("結帳成功時將狀態改為 CHECKED_OUT")
	void checkoutCart_updatesStatusToCheckedOut() {
		Cart cart = activeCart(100L, 1L);
		List<CartItem> items = List.of(
				cartItem(1L, 100L, 10L, 2)
		);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartId(100L)).thenReturn(items);
		when(cartRepository.save(cart)).thenReturn(cart);

		CartResponse response = cartService.checkoutCart(1L);

		assertEquals(Cart.CartStatus.CHECKED_OUT, cart.getStatus());
		assertEquals(Cart.CartStatus.CHECKED_OUT, response.status());
		assertEquals(1, response.items().size());
		InOrder checkoutOrder = inOrder(
				cartRepository,
				cartItemRepository
		);
		checkoutOrder.verify(cartRepository).findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		);
		checkoutOrder.verify(cartItemRepository).findByCartId(100L);
		checkoutOrder.verify(cartRepository).save(cart);
	}

	@Test
	@DisplayName("找不到 ACTIVE Cart 時結帳失敗")
	void checkoutCart_throwsExceptionWhenActiveCartDoesNotExist() {
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());

		CartNotFoundException exception = assertThrows(
				CartNotFoundException.class,
				() -> cartService.checkoutCart(1L)
		);

		assertTrue(exception.getMessage().contains("userId：1"));
		verifyNoInteractions(cartItemRepository);
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("空購物車結帳時失敗")
	void checkoutCart_throwsExceptionWhenCartIsEmpty() {
		Cart cart = activeCart(100L, 1L);
		when(cartRepository.findByUserIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartId(100L)).thenReturn(List.of());

		EmptyCartException exception = assertThrows(
				EmptyCartException.class,
				() -> cartService.checkoutCart(1L)
		);

		assertTrue(exception.getMessage().contains("cartId：100"));
		verify(cartRepository, never()).save(any(Cart.class));
	}

	private Cart activeCart(Long cartId, Long userId) {
		return Cart.builder()
				.cartId(cartId)
				.userId(userId)
				.status(Cart.CartStatus.ACTIVE)
				.createdAt(FIXED_TIME)
				.updatedAt(FIXED_TIME)
				.build();
	}

	private CartItem cartItem(
			Long cartItemId,
			Long cartId,
			Long productId,
			Integer quantity
	) {
		return CartItem.builder()
				.cartItemId(cartItemId)
				.cartId(cartId)
				.productId(productId)
				.quantity(quantity)
				.createdAt(FIXED_TIME)
				.updatedAt(FIXED_TIME)
				.build();
	}
}
