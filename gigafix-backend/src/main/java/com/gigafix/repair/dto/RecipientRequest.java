package com.gigafix.repair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecipientRequest {
// 技師編輯收件人資訊：結案前都可以改，僅限「寄件(COURIER)」的維修單才能用

	@NotNull(message = "請提供技師ID，用來確認是不是本人負責的維修單")
	private Integer technicianId;

	@NotNull(message = "請填寫收件人姓名")
	@Size(max = 50)
	private String recipientName;

	@NotNull(message = "請填寫收件人電話")
	@Size(max = 20)
	private String recipientPhone;

	@NotNull(message = "請填寫收件地址")
	@Size(max = 200)
	private String recipientAddress;

}
