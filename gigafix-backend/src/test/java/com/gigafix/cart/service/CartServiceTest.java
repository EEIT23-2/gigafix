package com.gigafix.cart.service;

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

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.CartNotModifiableException;
import com.gigafix.cart.exception.DuplicateCartItemException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

/** 驗證購物車唯一商品、ownership 與 ACTIVE 狀態限制。 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {
	@Mock CartRepository cartRepository;
	@Mock CartItemRepository cartItemRepository;
	@Mock MemberRepository memberRepository;
	private CartService service;
	private Member member;
	private Cart cart;

	@BeforeEach
	void setUp() {
		service = new CartService(cartRepository, cartItemRepository, memberRepository);
		member = Member.builder().id(1L).build();
		cart = Cart.builder().cartId(10L).member(member).status(Cart.CartStatus.ACTIVE).build();
	}

	@Test
	void activeCartCanAddUniqueProductWithoutQuantity() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartIdAndProductId(10L, 20L)).thenReturn(Optional.empty());
		when(cartItemRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
		assertEquals(20L, service.addCartItem(1L, new AddCartItemRequest(20L)).productId());
	}

	@Test
	void createsActiveCartForMemberWithoutOne() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.empty());
		when(cartRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> { Cart c=i.getArgument(0); c.setCartId(10L); return c; });
		when(cartItemRepository.findByCartCartIdAndProductId(10L, 20L)).thenReturn(Optional.empty());
		when(cartItemRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
		service.addCartItem(1L, new AddCartItemRequest(20L));
		verify(cartRepository).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void duplicateProductIsRejected() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartIdAndProductId(10L, 20L)).thenReturn(Optional.of(CartItem.builder().cart(cart).productId(20L).build()));
		assertThrows(DuplicateCartItemException.class, () -> service.addCartItem(1L, new AddCartItemRequest(20L)));
		verify(cartItemRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void deleteUsesOwnershipQuery() {
		CartItem item = CartItem.builder().cartItemId(30L).cart(cart).productId(20L).build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(30L, 1L)).thenReturn(Optional.of(item));
		service.deleteCartItem(1L, 30L);
		verify(cartItemRepository).delete(item);
	}

	@Test
	void crossMemberDeleteDoesNotRevealItem() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(30L, 1L)).thenReturn(Optional.empty());
		assertThrows(CartItemNotFoundException.class, () -> service.deleteCartItem(1L, 30L));
	}

	@Test
	void inactiveCartCannotBeMutated() {
		cart.setStatus(Cart.CartStatus.CHECKED_OUT);
		CartItem item = CartItem.builder().cartItemId(30L).cart(cart).productId(20L).build();
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartItemRepository.findByCartItemIdAndCartMemberId(30L, 1L)).thenReturn(Optional.of(item));
		assertThrows(CartNotModifiableException.class, () -> service.deleteCartItem(1L, 30L));
	}

	@Test
	void clearDeletesOnlyItemsAndAllowsEmptyCart() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
		service.clearActiveCart(1L);
		verify(cartItemRepository).deleteByCartCartId(10L);
		verify(cartRepository, never()).delete(cart);
	}

	@Test
	void missingActiveCartReturnsNotFound() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.empty());
		assertThrows(CartNotFoundException.class, () -> service.getActiveCart(1L));
	}

	@Test
	void getCartReturnsItemsWithoutQuantity() {
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(cartRepository.findByMemberIdAndStatus(1L, Cart.CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(CartItem.builder().cartItemId(30L).cart(cart).productId(20L).build()));
		assertEquals(20L, service.getActiveCart(1L).items().getFirst().productId());
	}
}
