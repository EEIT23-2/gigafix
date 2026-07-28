package com.gigafix.cart.exception;

/**
 * Order Service 尚未能取得完整商品資料，為避免建立不完整訂單時拋出，
 * 再由 CartExceptionHandler 轉為 501 回應。
 */
public class CheckoutNotAvailableException extends RuntimeException {

	public CheckoutNotAvailableException() {
		super("商品模組尚未完成，暫時無法建立訂單");
	}
}
