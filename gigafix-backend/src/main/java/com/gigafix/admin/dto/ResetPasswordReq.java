package com.gigafix.admin.dto;


public record ResetPasswordReq(
		String newPassword,
		Integer id
		) {

}
