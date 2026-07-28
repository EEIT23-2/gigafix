package com.gigafix.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	Optional<Order> findByOrderIdAndMemberId(
			Long orderId,
			Long memberId
	);

	List<Order> findByStatusOrderByCreatedAtDesc(
			Order.OrderStatus status
	);
}
