package com.gigafix.repair.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 匯出匯入功能：格式參數不對(不是json/xml/xlsx)，或上傳的檔案內容解析不出來時拋出
@ResponseStatus(HttpStatus.BAD_REQUEST) //400
public class InvalidFileFormatException extends RuntimeException {

	public InvalidFileFormatException(String message) {
		super(message);
	}

	public InvalidFileFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
