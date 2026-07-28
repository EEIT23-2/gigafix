package com.gigafix.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gigafix.cart.entity.Cart;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Long> {

	Optional<Cart> findByMemberIdAndStatus(
			Long memberId,
			Cart.CartStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT cart
			FROM Cart cart
			WHERE cart.member.id = :memberId
			  AND cart.status = :status
			""")
	Optional<Cart> findForCheckoutByMemberIdAndStatus(
			@Param("memberId") Long memberId,
			@Param("status") Cart.CartStatus status
	);

	List<Cart> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	boolean existsByMemberIdAndStatus(
			Long memberId,
			Cart.CartStatus status
	);
}
