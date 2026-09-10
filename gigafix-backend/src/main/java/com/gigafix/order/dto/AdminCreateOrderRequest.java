package com.gigafix.order.dto;

import java.util.List;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 管理員建立訂單的請求資料
public class AdminCreateOrderRequest {
    @NotNull(message = "會員ID不能為空")
    private Long memberId;//會員
    @NotEmpty(message = "商品ID清單不能為空")
    private List<Long> productIds;// 商品ID清單
    @NotBlank(message = "付款方式不能為空")
    @Pattern(regexp = "CREDIT_CARD", message = "目前僅支援信用卡付款")
    private String paymentMethod;// 付款方式
    @NotBlank(message = "收件人姓名不能為空")
    private String receiverName;// 收件人姓名
    @NotBlank(message = "收件人電話不能為空")
    private String receiverPhone;// 收件人電話
    @NotBlank(message = "收件地址不能為空")
    private String receiverAddress;// 收件地址
    @NotBlank(message = "配送方式不能為空")
    private String shippingMethod;// 配送方式
    private String customerRemark;// 客戶備註
}