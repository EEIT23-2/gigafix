package com.gigafix.product.dto;

import com.gigafix.product.constant.ProductCategory;
import jakarta.validation.constraints.NotNull;


public class ProductRequest {
    @NotNull
    private String productName; //品名(包含型號)
    @NotNull
    private ProductCategory category;  //之後要用enum列舉分類:手機 手錶 ipad 之後型別要改成 ProductCategory 暫用String代替

    private String imageUrl; //圖檔連結
    @NotNull
    private String description; //商品備註
    @NotNull
    private String appearance; //外觀狀況
    @NotNull
    private String grade;  //等級
    @NotNull
    private Integer price; //價格
    @NotNull
    private Integer saleStatus; //銷售(庫存)狀況 之後用enum常數列舉

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getSaleStatus() {
        return saleStatus;
    }

    public void setSaleStatus(Integer saleStatus) {
        this.saleStatus = saleStatus;
    }
}
