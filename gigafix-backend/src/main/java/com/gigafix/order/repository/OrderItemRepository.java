package com.gigafix.order.repository;

import com.gigafix.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // 查詢指定訂單的所有訂單明細
    List<OrderItem> findByOrderId(Long orderId);
    
}
