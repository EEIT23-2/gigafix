package com.gigafix.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.cart.entity.CartItem;

/**
 * 購物車項目資料存取介面，主要操作 {@code cart_items} 資料表。
 * 供 {@code CartService} 依購物車、商品及會員歸屬查詢或刪除 CartItem Entity。
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	/**
	 * 查詢指定購物車中的所有商品項目。
	 *
	 * @param cartId 購物車識別碼
	 * @return 購物車項目清單
	 */
	List<CartItem> findByCartCartId(Long cartId);

	/**
	 * 查詢指定購物車中的特定商品，用來判斷加入商品時是否應累加數量。
	 *
	 * @param cartId 購物車識別碼
	 * @param productId 商品識別碼
	 * @return 符合條件的購物車項目
	 */
	Optional<CartItem> findByCartCartIdAndProductId(
			Long cartId,
			Long productId
	);

	/**
	 * 依項目與會員識別碼查詢，確認該購物車項目確實屬於目前會員。
	 *
	 * @param cartItemId 購物車項目識別碼
	 * @param memberId 會員識別碼
	 * @return 由該會員擁有的購物車項目
	 */
	Optional<CartItem> findByCartItemIdAndCartMemberId(
			Long cartItemId,
			Long memberId
	);

	/**
	 * 刪除指定購物車中的全部項目。
	 *
	 * @param cartId 購物車識別碼
	 */
	void deleteByCartCartId(Long cartId);
}
