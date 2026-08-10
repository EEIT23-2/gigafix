package com.gigafix.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
//Request 前端送出訂單所需要的資訊
@Getter
@Setter
public class CreateOrderRequest {
    @NotBlank(message = "付款方式不能為空")
    private String paymentMethod;
    @NotBlank(message = "收件人姓名不能為空")
    private String receiverName;
    @NotBlank(message = "收件人電話不能為空")
    private String receiverPhone;
    @NotBlank(message = "收件人地址不能為空")
    private String receiverAddress;
    @NotBlank(message = "物流方式不能為空")
    private String shippingMethod;
    private String customerRemark; // 客戶備註 允許NULL
}
