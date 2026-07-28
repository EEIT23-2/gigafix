package com.gigafix.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.order.entity.OrderItem;

/**
 * 訂單項目資料存取介面，主要操作 {@code order_items} 資料表。
 * 供 {@code OrderService} 取得 Order 的明細，或在刪除訂單前移除相關 OrderItem Entity。
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	/**
	 * 查詢指定訂單的全部商品明細。
	 *
	 * @param orderId 訂單識別碼
	 * @return 訂單項目清單
	 */
	List<OrderItem> findByOrderOrderId(Long orderId);

	/**
	 * 刪除指定訂單的全部商品明細。
	 *
	 * @param orderId 訂單識別碼
	 */
	void deleteByOrderOrderId(Long orderId);
}
