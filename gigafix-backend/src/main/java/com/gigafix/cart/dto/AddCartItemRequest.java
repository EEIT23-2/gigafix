package com.gigafix.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

//Request 前端送出加入購物車所需要的資訊
@Getter
@Setter
public class AddCartItemRequest {

    // 商品 ID，不可為空且必須大於 0
    @NotNull(message = "productId 不可為空")
    @Positive(message = "productId 必須大於 0")
    private Long productId;
}