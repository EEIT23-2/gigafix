package com.gigafix.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminOrderCreateOptionsResponse {

    // 會員下拉選單
    private List<MemberOption> members;

    // 商品下拉選單
    private List<ProductOption> products;

    // 會員選項
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberOption {

        private Long memberId;
        private String memberName;
        private String phone;
        private String address;
    }

    // 商品選項
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProductOption {

        private Long productId;
        private String productName;
        private Integer price;
    }
}