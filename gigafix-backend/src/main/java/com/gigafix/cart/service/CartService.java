package com.gigafix.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartMemberNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.user.entity.Member;
import com.gigafix.user.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final MemberRepository memberRepository;

	public CartItemResponse addCartItem(
			Long memberId,
			AddCartItemRequest request
	) {
		Member member = findMember(memberId);
		validateQuantity(request.quantity());

		Cart cart = cartRepository.findByMemberIdAndStatus(
				memberId,
				Cart.CartStatus.ACTIVE
		).orElseGet(() -> cartRepository.save(Cart.builder()
				.member(member)
				.status(Cart.CartStatus.ACTIVE)
				.build()));

		CartItem cartItem = cartItemRepository
				.findByCartCartIdAndProductId(
						cart.getCartId(),
						request.productId()
				)
				.map(existingItem -> {
					existingItem.setQuantity(
							existingItem.getQuantity() + request.quantity()
					);
					return existingItem;
				})
				.orElseGet(() -> CartItem.builder()
						.cart(cart)
						.productId(request.productId())
						.quantity(request.quantity())
						.build());

		return toItemResponse(cartItemRepository.save(cartItem));
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public CartResponse getActiveCart(Long memberId) {
		findMember(memberId);
		Cart cart = findActiveCart(memberId);
		return toCartResponse(
				cart,
				cartItemRepository.findByCartCartId(cart.getCartId())
		);
	}

	public CartItemResponse updateQuantity(
			Long memberId,
			Long cartItemId,
			UpdateCartItemRequest request
	) {
		findMember(memberId);
		validateQuantity(request.quantity());

		CartItem cartItem = findOwnedCartItem(memberId, cartItemId);
		cartItem.setQuantity(request.quantity());
		return toItemResponse(cartItemRepository.save(cartItem));
	}

	public void deleteCartItem(Long memberId, Long cartItemId) {
		findMember(memberId);
		cartItemRepository.delete(findOwnedCartItem(memberId, cartItemId));
	}

	private Member findMember(Long memberId) {
		validateId(memberId, "memberId");
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new CartMemberNotFoundException(memberId));
	}

	private Cart findActiveCart(Long memberId) {
		return cartRepository.findByMemberIdAndStatus(
				memberId,
				Cart.CartStatus.ACTIVE
		).orElseThrow(() -> new CartNotFoundException(memberId));
	}

	private CartItem findOwnedCartItem(Long memberId, Long cartItemId) {
		validateId(cartItemId, "cartItemId");
		return cartItemRepository.findByCartItemIdAndCartMemberId(
				cartItemId,
				memberId
		).orElseThrow(() -> new CartItemNotFoundException(cartItemId));
	}

	private CartItemResponse toItemResponse(CartItem cartItem) {
		return new CartItemResponse(
				cartItem.getCartItemId(),
				cartItem.getCart().getCartId(),
				cartItem.getProductId(),
				cartItem.getQuantity(),
				cartItem.getCreatedAt(),
				cartItem.getUpdatedAt()
		);
	}

	private CartResponse toCartResponse(Cart cart, List<CartItem> items) {
		return new CartResponse(
				cart.getCartId(),
				cart.getMember().getId(),
				cart.getStatus(),
				cart.getCreatedAt(),
				cart.getUpdatedAt(),
				items.stream().map(this::toItemResponse).toList()
		);
	}

	private void validateId(Long id, String fieldName) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(fieldName + " 必須大於 0");
		}
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("購物車數量必須大於 0");
		}
	}
}
