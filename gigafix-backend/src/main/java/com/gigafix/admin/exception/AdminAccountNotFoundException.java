package com.gigafix.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminAccountNotFoundException extends AdminAccountException {
	public AdminAccountNotFoundException() {
		//直接在子類別建構式寫死資訊，不用每次throw這個錯誤的時候寫1次
		super("Admin_Not_Found", "該管理者不存在", HttpStatus.UNAUTHORIZED); //401
	}
}
