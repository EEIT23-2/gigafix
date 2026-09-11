package com.gigafix.order.repository;

import com.gigafix.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 查詢指定會員的所有訂單
    List<Order> findByMember_Id(Long memberId);

    // 檢查金流交易編號是否已被其他訂單使用
    boolean existsByTransactionId(String transactionId);
}
