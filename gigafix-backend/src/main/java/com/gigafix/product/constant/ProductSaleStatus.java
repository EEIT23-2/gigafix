package com.gigafix.product.constant;
//此類別用做給sale_status修改狀態
public enum ProductSaleStatus {
    OFF_SHELF(0, "已下架"),
    AVAILABLE(1, "可販售(待售中)"),
    RESERVED(2, "已保留"),
    SOLD(3, "已售出");

    private final int code;
    private final String description;

    ProductSaleStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
