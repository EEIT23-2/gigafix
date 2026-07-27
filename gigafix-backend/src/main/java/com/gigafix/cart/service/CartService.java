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
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.exception.InvalidCartQuantityException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;

	public CartItemResponse addCartItem(AddCartItemRequest request) {
		validateQuantity(request.quantity());

		Cart cart = cartRepository.findByUserIdAndStatus(
				request.userId(),
				Cart.CartStatus.ACTIVE
		).orElseGet(() -> cartRepository.save(Cart.builder()
				.userId(request.userId())
				.status(Cart.CartStatus.ACTIVE)
				.build()));

		CartItem cartItem = cartItemRepository
				.findByCartIdAndProductId(
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
						.cartId(cart.getCartId())
						.productId(request.productId())
						.quantity(request.quantity())
						.build());

		return toItemResponse(cartItemRepository.save(cartItem));
	}

	public CartResponse getActiveCart(Long userId) {
		validateUserId(userId);

		Cart cart = cartRepository.findByUserIdAndStatus(
				userId,
				Cart.CartStatus.ACTIVE
		).orElseThrow(() -> new CartNotFoundException(userId));
		List<CartItem> items = cartItemRepository.findByCartId(
				cart.getCartId()
		);

		return toCartResponse(cart, items);
	}

	public CartItemResponse updateQuantity(
			Long cartItemId,
			UpdateCartItemRequest request
	) {
		validateQuantity(request.quantity());

		CartItem cartItem = cartItemRepository.findById(cartItemId)
				.orElseThrow(() -> new CartItemNotFoundException(cartItemId));

		cartItem.setQuantity(request.quantity());

		return toItemResponse(cartItemRepository.save(cartItem));
	}

	public void deleteCartItem(Long cartItemId) {
		CartItem cartItem = cartItemRepository.findById(cartItemId)
				.orElseThrow(() -> new CartItemNotFoundException(cartItemId));

		cartItemRepository.delete(cartItem);
	}

	public CartResponse checkoutCart(Long userId) {
		validateUserId(userId);

		Cart cart = cartRepository.findByUserIdAndStatus(
				userId,
				Cart.CartStatus.ACTIVE
		).orElseThrow(() -> new CartNotFoundException(userId));
		List<CartItem> items = cartItemRepository.findByCartId(
				cart.getCartId()
		);
		if (items.isEmpty()) {
			throw new EmptyCartException(cart.getCartId());
		}

		cart.setStatus(Cart.CartStatus.CHECKED_OUT);
		Cart savedCart = cartRepository.save(cart);
		return toCartResponse(savedCart, items);
	}

	private CartItemResponse toItemResponse(CartItem cartItem) {
		return new CartItemResponse(
				cartItem.getCartItemId(),
				cartItem.getCartId(),
				cartItem.getProductId(),
				cartItem.getQuantity(),
				cartItem.getCreatedAt(),
				cartItem.getUpdatedAt()
		);
	}

	private CartResponse toCartResponse(
			Cart cart,
			List<CartItem> items
	) {
		return new CartResponse(
				cart.getCartId(),
				cart.getUserId(),
				cart.getStatus(),
				cart.getCreatedAt(),
				cart.getUpdatedAt(),
				items.stream().map(this::toItemResponse).toList()
		);
	}

	private void validateUserId(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("userId 必須大於 0");
		}
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new InvalidCartQuantityException("購物車數量必須大於 0");
		}
	}
}
