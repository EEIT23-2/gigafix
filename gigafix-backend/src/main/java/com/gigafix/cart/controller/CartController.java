package com.gigafix.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@PostMapping("/items")
	public ResponseEntity<CartItemResponse> addCartItem(
			@Valid @RequestBody AddCartItemRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(cartService.addCartItem(request));
	}

	@GetMapping("/users/{userId}/active")
	public ResponseEntity<CartResponse> getActiveCart(
			@PathVariable Long userId
	) {
		return ResponseEntity.ok(cartService.getActiveCart(userId));
	}

	@PutMapping("/items/{cartItemId}")
	public ResponseEntity<CartItemResponse> updateQuantity(
			@PathVariable Long cartItemId,
			@Valid @RequestBody UpdateCartItemRequest request
	) {
		return ResponseEntity.ok(
				cartService.updateQuantity(cartItemId, request)
		);
	}

	@DeleteMapping("/items/{cartItemId}")
	public ResponseEntity<Void> deleteCartItem(
			@PathVariable Long cartItemId
	) {
		cartService.deleteCartItem(cartItemId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/users/{userId}/checkout")
	public ResponseEntity<CartResponse> checkoutCart(
			@PathVariable Long userId
	) {
		return ResponseEntity.ok(cartService.checkoutCart(userId));
	}
}
