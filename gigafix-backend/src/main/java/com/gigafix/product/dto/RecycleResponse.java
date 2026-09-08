package com.gigafix.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.RecycleStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecycleResponse {

    private Long applyId;
    private Long memberId;
    private String memberName;
    private String contactPhone;
    private String productName;
    private ProductCategory category;
    private String appearance;
    private String imageUrl;
    private String description;
    private Integer estimatedPrice;
    private RecycleStatus recycleStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastModifiedTime;
    private Byte storeId;
    private String storeName;


}
