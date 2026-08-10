package com.gigafix.common.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ErrorResp(
		String errorCode,
        String message,
//        int status, //找步道欄位本來就是400
//        LocalDateTime timestamp,
//        String path,   //我沒有寫錯誤監控所以不需要
        List<FieldErrorDetail> errors
		) {}
