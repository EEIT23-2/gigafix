package com.gigafix.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminBusinessRuleCheckException extends AdminAccountException{
	public AdminBusinessRuleCheckException(String tellMeWhyAintNothinButAHeartache)  {
		//直接在子類別建構式寫死資訊，不用每次throw這個錯誤的時候寫1次
		super("The_Action_Against_Role", tellMeWhyAintNothinButAHeartache, HttpStatus.UNAUTHORIZED); //401
	}
}
