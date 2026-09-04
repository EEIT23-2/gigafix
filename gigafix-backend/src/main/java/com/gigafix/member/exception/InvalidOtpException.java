package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends MemberException {
	public InvalidOtpException() {
		//OTP驗證碼不相符或已逾期(快取內已找不到)都算這個錯誤，不特別區分給使用者知道原因，避免被拿來猜測驗證碼
		super("INVALID_OTP", "OTP驗證碼錯誤或已逾期，請重新取得驗證碼", HttpStatus.BAD_REQUEST); //400
	}
}
