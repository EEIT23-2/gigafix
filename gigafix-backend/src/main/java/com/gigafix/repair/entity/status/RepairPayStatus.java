package com.gigafix.repair.entity.status;

public enum RepairPayStatus {
	// 付款狀態
	// 會有 null 的狀態

	UNPAID, // 0 未付款
	PAID, // 1 已付款
	REFUNDED, // 2 已退款
	PENDING // 3 付款中/待確認(線上付款送出後、綠界回調確認前的中間狀態)。
	// 用EnumType.ORDINAL存，新增值一定要加在最後面，不能插在中間，否則舊資料的付款狀態會全部錯位

}
