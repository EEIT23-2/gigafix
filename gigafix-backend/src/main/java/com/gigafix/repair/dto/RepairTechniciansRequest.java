package com.gigafix.repair.dto;

import lombok.Data;

@Data
public class RepairTechniciansRequest {

	private String name;
	private String phone;
	private Byte storeId; // 前端傳入分店 ID
}
