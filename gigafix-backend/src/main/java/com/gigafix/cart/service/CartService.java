package com.gigafix.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.InvalidCartQuantityException;
import com.gigafix.cart.repository.CartRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;

	public CartItemResponse addCartItem(AddCartItemRequest request) {
		validateQuantity(request.quantity());

		CartItem cartItem = cartRepository
				.findByUserIdAndProductId(request.userId(), request.productId())
				.map(existingItem -> {
					existingItem.setQuantity(
							existingItem.getQuantity() + request.quantity()
					);
					return existingItem;
				})
				.orElseGet(() -> CartItem.builder()
						.userId(request.userId())
						.productId(request.productId())
						.quantity(request.quantity())
						.build());

		return toResponse(cartRepository.save(cartItem));
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<CartItemResponse> getCartItems(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("userId 必須大於 0");
		}

		return cartRepository.findByUserId(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	public CartItemResponse updateQuantity(
			Long cartItemId,
			UpdateCartItemRequest request
	) {
		validateQuantity(request.quantity());

		CartItem cartItem = cartRepository.findById(cartItemId)
				.orElseThrow(() -> new CartItemNotFoundException(cartItemId));

		cartItem.setQuantity(request.quantity());

		return toResponse(cartRepository.save(cartItem));
	}

	public void deleteCartItem(Long cartItemId) {
		CartItem cartItem = cartRepository.findById(cartItemId)
				.orElseThrow(() -> new CartItemNotFoundException(cartItemId));

		cartRepository.delete(cartItem);
	}

	private CartItemResponse toResponse(CartItem cartItem) {
		return new CartItemResponse(
				cartItem.getCartItemId(),
				cartItem.getUserId(),
				cartItem.getProductId(),
				cartItem.getQuantity(),
				cartItem.getCreatedAt(),
				cartItem.getUpdatedAt()
		);
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new InvalidCartQuantityException("購物車數量必須大於 0");
		}
	}
}
