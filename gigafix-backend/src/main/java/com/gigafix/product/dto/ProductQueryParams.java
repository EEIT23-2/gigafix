package com.gigafix.product.dto;

import com.gigafix.product.constant.ProductCategory;
import org.springframework.stereotype.Component;
//此class用做統整 條件查詢參數



public class ProductQueryParams {
    //查詢條件
    private ProductCategory category; //類別查詢
    private String search; //品名查詢
    private String modelName; //型號查詢
    private String color; //顏色查詢
    private String storage; //容量查詢
    //排序條件
    private String orderBy;//依 初始值建立時間排列查詢
    private String sort;//依降冪排列查詢

    //按價格區間查詢
    private Integer minPrice;//取低價參數
    private Integer maxPrice;//取高價參數

    //分頁功能 所需參數
    private Integer limit;
    private Integer offset;


    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
