package com.gigafix.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RepairTechniciansRequest {

	@NotBlank(message = "請填寫技師姓名")
	@Size(max= 20)
	private String name;
	
	@NotBlank(message = "請填寫技師電話")
	@Size(max = 20)
	private String phone;
	
	@NotBlank(message = "請選擇所屬分店" )
	private Byte storeId; // 前端傳入分店 ID
}
