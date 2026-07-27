package com.gigafix.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.cart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

	Optional<Cart> findByUserIdAndStatus(
			Long userId,
			Cart.CartStatus status
	);

	List<Cart> findByUserIdOrderByCreatedAtDesc(Long userId);

	boolean existsByUserIdAndStatus(
			Long userId,
			Cart.CartStatus status
	);
}
