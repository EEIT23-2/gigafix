package com.gigafix.repair.dto;

import com.gigafix.repair.entity.status.PickupType;
import com.gigafix.repair.entity.status.RepairPay;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PickupPaymentRequest {
// 客戶選取件方式＋付款方式，只能送出一次，送出後如需更動要請技師改
// 選「寄件(COURIER)」才需要填收件人姓名/電話/地址，Service層檢查

	@NotNull(message = "請選擇取件方式")
	private PickupType pickupType;

	@NotNull(message = "請選擇付款方式")
	private RepairPay repairPay;

	@Size(max = 50)
	private String recipientName;

	@Size(max = 20)
	private String recipientPhone;

	@Size(max = 200)
	private String recipientAddress;

}
