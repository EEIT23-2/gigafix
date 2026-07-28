package com.gigafix.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrderOrderId(Long orderId);

	void deleteByOrderOrderId(Long orderId);
}
