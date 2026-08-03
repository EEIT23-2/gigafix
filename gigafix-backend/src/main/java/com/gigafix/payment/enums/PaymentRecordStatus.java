package com.gigafix.payment.enums;

/**
 * 表示單筆 Payment 的處理狀態；Order PaymentStatus 則是訂單層級的付款摘要。
 */
public enum PaymentRecordStatus {
	PENDING, // 等待付款結果
	PAID, // 付款成功
	PAYMENT_FAILED, // 付款失敗
	REFUNDED, // 已退款
	CANCELLED // 付款紀錄已取消
}
