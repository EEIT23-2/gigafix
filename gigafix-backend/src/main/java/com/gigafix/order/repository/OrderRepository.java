package com.gigafix.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.order.entity.Order;

/**
 * 訂單資料存取介面，主要操作 {@code orders} 資料表。
 * 供 {@code OrderService} 依會員、訂單識別碼或狀態查詢 Order Entity。
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

	/**
	 * 查詢會員的全部訂單，並依建立時間由新到舊排列。
	 *
	 * @param memberId 會員識別碼
	 * @return 會員的訂單清單
	 */
	List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	/**
	 * 依訂單與會員識別碼查詢，用來確認訂單屬於目前會員。
	 *
	 * @param orderId 訂單識別碼
	 * @param memberId 會員識別碼
	 * @return 由該會員擁有的訂單
	 */
	Optional<Order> findByOrderIdAndMemberId(
			Long orderId,
			Long memberId
	);

	/**
	 * 查詢指定狀態的訂單，並依建立時間由新到舊排列。
	 *
	 * @param status 訂單處理狀態
	 * @return 符合狀態的訂單清單
	 */
	List<Order> findByStatusOrderByCreatedAtDesc(
			Order.OrderStatus status
	);
}
