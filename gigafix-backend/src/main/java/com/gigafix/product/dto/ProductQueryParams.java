package com.gigafix.product.dto;

import com.gigafix.product.constant.ProductCategory;
import org.springframework.stereotype.Component;
//此class用做統整 條件查詢參數



public class ProductQueryParams {

    private ProductCategory category; //類別查詢
    private String search; //品名查詢
    private String modelName; //型號查詢
    private String color; //顏色查詢
    private String storage; //容量查詢

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
}
