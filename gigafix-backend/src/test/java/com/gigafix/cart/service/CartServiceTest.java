package com.gigafix.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@Mock
	private CartRepository cartRepository;

	@InjectMocks
	private CartService cartService;

	@Test
	@DisplayName("商品不存在時建立新購物車項目")
	void addCartItem_createsNewItemWhenProductDoesNotExist() {
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		when(cartRepository.findByUserIdAndProductId(1L, 10L))
				.thenReturn(Optional.empty());
		when(cartRepository.save(any(CartItem.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CartItemResponse response = cartService.addCartItem(request);

		ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
		verify(cartRepository, times(1)).save(captor.capture());
		CartItem savedItem = captor.getValue();

		assertEquals(1L, savedItem.getUserId());
		assertEquals(10L, savedItem.getProductId());
		assertEquals(2, savedItem.getQuantity());
		assertEquals(1L, response.userId());
		assertEquals(10L, response.productId());
		assertEquals(2, response.quantity());
	}

	@Test
	@DisplayName("商品已存在時累加購物車數量")
	void addCartItem_increasesQuantityWhenProductExists() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
		LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 1, 10, 30);
		CartItem existingItem = CartItem.builder()
				.cartItemId(1L)
				.userId(1L)
				.productId(10L)
				.quantity(3)
				.createdAt(createdAt)
				.updatedAt(updatedAt)
				.build();
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		when(cartRepository.findByUserIdAndProductId(1L, 10L))
				.thenReturn(Optional.of(existingItem));
		when(cartRepository.save(existingItem)).thenReturn(existingItem);

		CartItemResponse response = cartService.addCartItem(request);

		ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
		verify(cartRepository, times(1)).save(captor.capture());
		assertSame(existingItem, captor.getValue());
		assertEquals(5, existingItem.getQuantity());
		assertEquals(5, response.quantity());
	}

	@Test
	@DisplayName("回傳指定會員的購物車項目")
	void getCartItems_returnsItemsForSpecifiedUser() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
		LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 1, 10, 30);
		CartItem firstItem = CartItem.builder()
				.cartItemId(1L)
				.userId(1L)
				.productId(10L)
				.quantity(2)
				.createdAt(createdAt)
				.updatedAt(updatedAt)
				.build();
		CartItem secondItem = CartItem.builder()
				.cartItemId(2L)
				.userId(1L)
				.productId(20L)
				.quantity(4)
				.createdAt(createdAt)
				.updatedAt(updatedAt)
				.build();
		when(cartRepository.findByUserId(1L))
				.thenReturn(List.of(firstItem, secondItem));

		List<CartItemResponse> responses = cartService.getCartItems(1L);

		assertEquals(2, responses.size());
		assertTrue(responses.stream().allMatch(response -> response.userId() == 1L));
		verify(cartRepository, times(1)).findByUserId(1L);
	}

	@ParameterizedTest(name = "userId = {0}")
	@NullSource
	@ValueSource(longs = {0L, -1L})
	@DisplayName("userId 不合法時拋出例外")
	void getCartItems_throwsExceptionWhenUserIdIsInvalid(Long userId) {
		assertThrows(
				IllegalArgumentException.class,
				() -> cartService.getCartItems(userId)
		);
		verifyNoInteractions(cartRepository);
	}

	@Test
	@DisplayName("成功修改購物車數量")
	void updateQuantity_updatesQuantitySuccessfully() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
		LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 1, 10, 30);
		CartItem existingItem = CartItem.builder()
				.cartItemId(1L)
				.userId(1L)
				.productId(10L)
				.quantity(2)
				.createdAt(createdAt)
				.updatedAt(updatedAt)
				.build();
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		when(cartRepository.findById(1L)).thenReturn(Optional.of(existingItem));
		when(cartRepository.save(existingItem)).thenReturn(existingItem);

		CartItemResponse response = cartService.updateQuantity(1L, request);

		ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
		verify(cartRepository, times(1)).save(captor.capture());
		assertSame(existingItem, captor.getValue());
		assertEquals(5, captor.getValue().getQuantity());
		assertEquals(1L, response.cartItemId());
		assertEquals(5, response.quantity());
	}

	@Test
	@DisplayName("修改數量時找不到項目則拋出例外")
	void updateQuantity_throwsExceptionWhenItemDoesNotExist() {
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		when(cartRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(
				CartItemNotFoundException.class,
				() -> cartService.updateQuantity(1L, request)
		);
		verify(cartRepository, never()).save(any(CartItem.class));
	}

	@Test
	@DisplayName("成功刪除購物車項目")
	void deleteCartItem_deletesItemSuccessfully() {
		CartItem existingItem = CartItem.builder()
				.cartItemId(1L)
				.userId(1L)
				.productId(10L)
				.quantity(2)
				.build();
		when(cartRepository.findById(1L)).thenReturn(Optional.of(existingItem));

		cartService.deleteCartItem(1L);

		verify(cartRepository, times(1)).delete(existingItem);
	}

	@Test
	@DisplayName("刪除時找不到項目則拋出例外")
	void deleteCartItem_throwsExceptionWhenItemDoesNotExist() {
		when(cartRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(
				CartItemNotFoundException.class,
				() -> cartService.deleteCartItem(1L)
		);
		verify(cartRepository, never()).delete(any(CartItem.class));
	}
}
