package com.gigafix.product.repository;

import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.RecycleStatus;
import com.gigafix.product.dto.RecycleResponse;
import com.gigafix.product.entity.RecycleApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecycleApplicationDao extends JpaRepository<RecycleApplication,Long> {
    //用JPQL實作條件查詢

    @Query("SELECT r FROM RecycleApplication r WHERE " +
            "(:memberId IS NULL OR r.member.id = :memberId) AND " +
            "(:productName IS NULL OR r.productName LIKE %:productName%) AND " +
            "(:appearance IS NULL OR r.appearance LIKE %:appearance%) AND " +
            "(:category IS NULL OR r.category = :category) AND " +
            "(:recycleStatus IS NULL OR r.recycleStatus = :recycleStatus)"
    )
    Page<RecycleApplication>findByConditions(   @Param("memberId") Long memberId,
                                                @Param("productName") String productName,
                                                @Param("appearance") String appearance,
                                                @Param("category") ProductCategory category,
                                                @Param("recycleStatus") RecycleStatus recycleStatus,
                                                Pageable page);

}
