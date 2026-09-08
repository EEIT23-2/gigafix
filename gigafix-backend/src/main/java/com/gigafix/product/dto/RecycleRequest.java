package com.gigafix.product.dto;

import com.gigafix.product.constant.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RecycleRequest {

    @NotNull(message = "會員 ID 不可為空")  //最後確定跟spring security連動的時候 這個屬性要註釋掉
    private Long memberId;//目前for開發用

    @NotBlank(message = "商品名稱不可為空")
    private String productName;

    @NotNull(message = "商品分類不可為空")
    private ProductCategory category;

    @NotBlank(message = "商品外觀不可為空")
    private String appearance;

    private String imageUrl;

    private String description;

    @PositiveOrZero(message = "預估價格不可小於 0")
    private Integer estimatedPrice;

    private Byte storeId;
}
