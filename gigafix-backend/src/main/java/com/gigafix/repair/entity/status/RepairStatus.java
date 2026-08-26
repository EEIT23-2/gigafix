package com.gigafix.repair.entity.status;

public enum RepairStatus {
//	維修狀態
	
	PENDING_QUOTE,           // 0 待估價
	QUOTED,                  // 1 已報價
	IN_REPAIR,               // 2 維修中
	QUOTE_REJECTED,          // 3 報價後不維修
	REPAIR_COMPLETED,        // 4 維修完成
	AWAITING_PICKUP,         // 5 電話通知後:尚未至門市取件/(用寄的)尚未取貨
	CLOSED,                  // 6 結案（顧客已取件/已收到貨/報價後不維修）
	CANCELLED,               // 7 已取消
	NOT_DROPPED_OFF          // 8 未送檢
	
//	目前先只有到店取貨付款

}
