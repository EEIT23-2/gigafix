package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMeNameReq(
		@NotBlank(message = "admin名稱不可為空")
		@Size(max = 20, message = "名稱字數上限為20個字元")
		String newName) {

}
