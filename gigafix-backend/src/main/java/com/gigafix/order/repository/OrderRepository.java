package com.gigafix.order.repository;


import com.gigafix.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 查詢指定會員的所有訂單
    List<Order> findByMemberId(Long memberId);
    

}
