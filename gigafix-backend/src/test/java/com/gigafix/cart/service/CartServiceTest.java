package com.gigafix.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartMemberNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.user.entity.Member;
import com.gigafix.user.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@Mock
	private MemberRepository memberRepository;

	private CartService cartService;
	private Member member;
	private Cart cart;

	@BeforeEach
	void setUp() {
		cartService = new CartService(
				cartRepository,
				cartItemRepository,
				memberRepository
		);
		member = Member.builder().id(1L).build();
		cart = Cart.builder()
				.cartId(100L)
				.member(member)
				.status(Cart.CartStatus.ACTIVE)
				.build();
	}

	@Test
	void addCartItem_requiresExistingMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(
				CartMemberNotFoundException.class,
				() -> cartService.addCartItem(
						99L,
						new AddCartItemRequest(10L, 1)
				)
		);
		verify(cartRepository, never())
				.findByMemberIdAndStatus(any(), any());
	}

	@Test
	void addCartItem_createsCartWithManagedMember() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);
		when(cartItemRepository.findByCartCartIdAndProductId(100L, 10L))
				.thenReturn(Optional.empty());
		when(cartItemRepository.save(any(CartItem.class)))
				.thenAnswer(invocation -> {
					CartItem item = invocation.getArgument(0);
					item.setCartItemId(200L);
					return item;
				});

		var response = cartService.addCartItem(
				1L,
				new AddCartItemRequest(10L, 2)
		);

		assertEquals(100L, response.cartId());
		assertEquals(10L, response.productId());
		assertEquals(2, response.quantity());
	}

	@Test
	void getActiveCart_requiresExistingMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(
				CartMemberNotFoundException.class,
				() -> cartService.getActiveCart(99L)
		);
	}

	@Test
	void getActiveCart_returnsOnlyRequestedMembersCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartId(100L))
				.thenReturn(List.of());

		var response = cartService.getActiveCart(1L);

		assertEquals(1L, response.memberId());
		verify(cartRepository).findByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		);
	}

	@Test
	void updateQuantity_rejectsAnotherMembersCartItem() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(200L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(
				CartItemNotFoundException.class,
				() -> cartService.updateQuantity(
						1L,
						200L,
						new UpdateCartItemRequest(3)
				)
		);
	}

	@Test
	void deleteCartItem_rejectsAnotherMembersCartItem() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(200L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(
				CartItemNotFoundException.class,
				() -> cartService.deleteCartItem(1L, 200L)
		);
		verify(cartItemRepository, never()).delete(any());
	}

	@Test
	void addCartItemIncreasesExistingQuantity() {
		CartItem existing = CartItem.builder()
				.cartItemId(200L)
				.cart(cart)
				.productId(10L)
				.quantity(2)
				.build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartIdAndProductId(100L, 10L))
				.thenReturn(Optional.of(existing));
		when(cartItemRepository.save(existing)).thenReturn(existing);

		var response = cartService.addCartItem(
				1L,
				new AddCartItemRequest(10L, 3)
		);

		assertEquals(5, response.quantity());
	}

	@Test
	void getActiveCartRejectsMissingCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(
				1L,
				Cart.CartStatus.ACTIVE
		)).thenReturn(Optional.empty());

		assertThrows(
				CartNotFoundException.class,
				() -> cartService.getActiveCart(1L)
		);
	}

	@Test
	void updateQuantityUpdatesOwnedItem() {
		CartItem item = CartItem.builder()
				.cartItemId(200L)
				.cart(cart)
				.productId(10L)
				.quantity(1)
				.build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(200L, 1L))
				.thenReturn(Optional.of(item));
		when(cartItemRepository.save(item)).thenReturn(item);

		var response = cartService.updateQuantity(
				1L,
				200L,
				new UpdateCartItemRequest(4)
		);

		assertEquals(4, response.quantity());
	}

	@Test
	void deleteCartItemDeletesOwnedItem() {
		CartItem item = CartItem.builder()
				.cartItemId(200L)
				.cart(cart)
				.productId(10L)
				.quantity(1)
				.build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(200L, 1L))
				.thenReturn(Optional.of(item));

		cartService.deleteCartItem(1L, 200L);

		verify(cartItemRepository).delete(item);
	}
}
