package com.gigafix.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gigafix.cart.entity.Cart;

import jakarta.persistence.LockModeType;

/**
 * 購物車資料存取介面，主要操作 {@code carts} 資料表。
 * 供 {@code CartService} 與結帳流程依會員、狀態或建立時間查詢 Cart Entity。
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

	/**
	 * 依會員與購物車狀態查詢單一購物車，主要用來取得會員目前的 ACTIVE 購物車。
	 *
	 * @param memberId 會員識別碼
	 * @param status 購物車狀態
	 * @return 符合條件的購物車
	 */
	Optional<Cart> findByMemberIdAndStatus(
			Long memberId,
			Cart.CartStatus status
	);

	/**
	 * 以悲觀寫入鎖取得會員指定狀態的購物車。
	 * 結帳期間鎖住同一筆資料，可避免多個請求同時重複結帳。
	 *
	 * @param memberId 會員識別碼
	 * @param status 要鎖定的購物車狀態
	 * @return 已鎖定且符合條件的購物車
	 */
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

	/**
	 * 查詢會員的購物車紀錄，並依建立時間由新到舊排列。
	 *
	 * @param memberId 會員識別碼
	 * @return 會員的購物車清單
	 */
	List<Cart> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	/**
	 * 判斷會員是否已有指定狀態的購物車。
	 *
	 * @param memberId 會員識別碼
	 * @param status 購物車狀態
	 * @return 存在時為 true，否則為 false
	 */
	boolean existsByMemberIdAndStatus(
			Long memberId,
			Cart.CartStatus status
	);
}
