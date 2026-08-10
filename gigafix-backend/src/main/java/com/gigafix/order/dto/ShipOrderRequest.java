package com.gigafix.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 訂單出貨請求 DTO
 * 接收物流追蹤編號
 */
@Getter
@Setter
@NoArgsConstructor
public class ShipOrderRequest {

    // 物流追蹤編號
    @NotBlank(message = "trackingNumber 不可空白")
    private String trackingNumber;
}