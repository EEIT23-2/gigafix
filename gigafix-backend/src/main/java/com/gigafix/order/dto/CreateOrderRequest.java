package com.gigafix.order.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

//Request 前端送出訂單所需要的資訊
@Getter
@Setter
public class CreateOrderRequest {
    @NotEmpty(message = "請至少選擇一項商品")
    private List<Long> cartItemIds;
    private String couponCode; // 使用的優惠券代碼
    @NotBlank(message = "付款方式不能為空")
    @Pattern(regexp = "CREDIT_CARD", message = "目前僅支援信用卡付款")
    private String paymentMethod;
    @NotBlank(message = "收件人姓名不能為空")
    private String receiverName;
    @NotBlank(message = "收件人電話不能為空")
    private String receiverPhone;
    private String receiverAddress; // 宅配使用
    @NotBlank(message = "物流方式不能為空")
    private String shippingMethod;
    // 超商取貨使用
    private String storeType;
    private String storeId;
    private String storeName;
    private String storeAddress;
    private String customerRemark; // 客戶備註 允許NULL
}
