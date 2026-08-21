package com.gigafix.order.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 管理員建立訂單的請求資料
public class AdminCreateOrderRequest {
    @NotNull
    private Long memberId;//會員
    @NotEmpty
    private List<Long> productIds;// 商品ID清單
    @NotBlank
    private String paymentMethod;// 付款方式
    @NotBlank
    private String receiverName;// 收件人姓名
    @NotBlank
    private String receiverPhone;// 收件人電話
    @NotBlank
    private String receiverAddress;// 收件地址
    @NotBlank
    private String shippingMethod;// 配送方式
    private String customerRemark;// 客戶備註
}