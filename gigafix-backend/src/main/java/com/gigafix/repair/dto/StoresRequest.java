package com.gigafix.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StoresRequest {
	
	@NotBlank(message = "請填寫分店名稱")
	@Size(max = 20)
	private String name;
	
	@NotBlank(message = "請填寫分店地址")
	@Size(max = 200)
	private String address;
	
	@NotBlank(message = "請填寫分店電話")
	@Size(max = 20)
	private String phone;
	

}
