package com.gigafix.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 更新訂單的請求資料
public class UpdateOrderRequest {
    // 收件人姓名
    @NotBlank(message = "收件人姓名不可為空")
    private String receiverName;
    // 收件人電話
    @NotBlank(message = "收件人電話不可為空")
    private String receiverPhone;
    // 收件人地址
    @NotBlank(message = "收件人地址不可為空")
    private String receiverAddress;
    // 運送方式
    @NotBlank(message = "運送方式不可為空")
    private String shippingMethod;
    // 客戶備註
    private String customerRemark;
}
