package com.gigafix.cart.repository;

import com.gigafix.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

        // 查詢某位會員購物車內的全部商品
        List<CartItem> findByMember_Id(Long memberId);// List可能有多筆資料

        // 查詢某位會員是否已經加入指定商品
        Optional<CartItem> findByMember_IdAndProductId(// Optional可能一筆或沒有資料
                        Long memberId,
                        Long productId);

        // 查詢指定會員的購物車商品
        Optional<CartItem> findByCartItemIdAndMember_Id( // Optional可能一筆或沒有資料
                        Long cartItemId,
                        Long memberId);
}