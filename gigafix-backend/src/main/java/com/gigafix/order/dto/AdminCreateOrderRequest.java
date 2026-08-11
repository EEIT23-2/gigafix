package com.gigafix.order.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 管理員建立訂單的請求資料
public class AdminCreateOrderRequest {

    private Long memberId;//會員
    private List<Long> productIds;// 商品ID清單
    private String paymentMethod;// 付款方式
    private String receiverName;// 收件人姓名
    private String receiverPhone;// 收件人電話
    private String receiverAddress;// 收件地址
    private String shippingMethod;// 配送方式
    private String customerRemark;// 客戶備註
}