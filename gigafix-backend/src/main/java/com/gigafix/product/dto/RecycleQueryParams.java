package com.gigafix.product.dto;

import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.RecycleStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RecycleQueryParams {
    private Long applyId;
    //查詢條件  dao要實作模糊查詢品名
    private Long memberId;
    private String productName;
    private String appearance;
    private ProductCategory productCategory;
    private RecycleStatus recycleStatus;
    //排序條件
    private String orderBy;//依 初始值建立時間排列查詢
    private String sort;//依降冪排列查詢

    //分頁功能 所需參數
    private Integer limit;
    private Integer offset;

}
