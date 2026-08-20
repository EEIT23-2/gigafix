package com.gigafix.repair.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoresResponse {
//	非必要，統一風格由DTO過濾
	
	private Byte id;
	private String name;
	private String address;
	private String phone;

}
