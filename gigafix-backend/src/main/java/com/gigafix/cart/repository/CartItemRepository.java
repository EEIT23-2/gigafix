package com.gigafix.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	List<CartItem> findByCartCartId(Long cartId);

	Optional<CartItem> findByCartCartIdAndProductId(
			Long cartId,
			Long productId
	);

	Optional<CartItem> findByCartItemIdAndCartMemberId(
			Long cartItemId,
			Long memberId
	);

	void deleteByCartCartId(Long cartId);
}
