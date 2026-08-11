package com.gigafix.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 付款成功請求 DTO
 * 接收金流交易編號
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentSuccessRequest {

    // 金流交易編號
    @NotBlank(message = "transactionId 不可空白")
    private String transactionId;
}