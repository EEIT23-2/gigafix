package com.gigafix.order.dto;

import java.util.List;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Positive(message = "會員 ID 必須為正數")
    private Long memberId;// 會員

    @NotEmpty(message = "商品 ID 清單不能為空")
    private List<@NotNull(message = "商品 ID 不能為空") @Positive(message = "商品 ID 必須為正數") Long> productIds;// 商品ID清單

    @NotBlank(message = "付款方式不能為空")
    @Pattern(regexp = "CREDIT_CARD", message = "目前僅支援信用卡付款")
    private String paymentMethod;// 付款方式

    @NotBlank(message = "收件人姓名不能為空")
    @Size(max = 40, message = "收件人姓名最多 40 個字")
    private String receiverName;// 收件人姓名

    @NotBlank(message = "收件人電話不能為空")
    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼必須為 09 開頭的 10 碼數字")
    private String receiverPhone;// 收件人電話

    @NotBlank(message = "收件地址不能為空")
    @Size(max = 200, message = "收件地址最多 200 個字")
    private String receiverAddress;// 收件地址

    @NotBlank(message = "配送方式不能為空")
    @Pattern(regexp = "HOME", message = "管理員新增訂單僅支援宅配")
    private String shippingMethod;// 配送方式
    
    @Size(max = 255, message = "訂單備註最多 255 個字")
    private String customerRemark;// 客戶備註
}