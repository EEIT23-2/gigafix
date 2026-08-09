package com.gigafix.payment.dto.request;

import com.gigafix.payment.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

/** 建立付款紀錄時由會員選擇的付款方式。 */
public record CreatePaymentRequest(@NotNull PaymentMethod paymentMethod) {
}
