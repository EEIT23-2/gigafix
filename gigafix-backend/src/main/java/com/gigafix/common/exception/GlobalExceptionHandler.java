package com.gigafix.common.exception;

import java.util.List;


import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.gigafix.admin.exception.AdminAccountException;
import com.gigafix.common.dto.ErrorResp;
import com.gigafix.common.dto.FieldErrorDetail;
import com.gigafix.member.exception.MemberException;

import tools.jackson.databind.exc.InvalidFormatException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResp> handleValidationException(MethodArgumentNotValidException e){
		List<FieldErrorDetail> fieldErrors= e.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(fieldError -> FieldErrorDetail.builder()
					.field(fieldError.getField())
					.message(fieldError.getDefaultMessage())
					.rejectedValue(fieldError.getRejectedValue())
					.build())
				.toList();  //這一段是因為MethodArgumentNotValidException可能會有多個認證錯誤的欄位，因此必須多一個List
		
		// 取第一個欄位錯誤的訊息當作主要提示訊息
	    String firstMessage = fieldErrors.isEmpty() ? "輸入格式有誤，請確認後再試一次" : fieldErrors.get(0).message();
		
		//badRequest() 等同於status(HttpStatus.BAD_REQUEST) 也就是status code400
		return ResponseEntity.badRequest().body(ErrorResp.builder()
										.errorCode("VALIDATION_FAILED")
										.message(firstMessage)
										.errors(fieldErrors).build());
	}
	/*ConstraintViolationException是@Valid使用在非controller時(非controller必須在class上方加上@Validated)，如果驗證失敗會throw這個錯誤
	 * 但目前使用@Validated的Bean是JwtUtils，而使用@Valid檢查的method會是工程師自己發出去的內容，如果報錯表示程式有漏洞，不該使用ExceptionHandler catch起來*/
	
	@ExceptionHandler(HttpMessageNotReadableException.class)//如果輸入的格式是我自己定義的enum但是前端傳錯的會拋出這個例外
	public ResponseEntity<ErrorResp> handleJsonParseException(HttpMessageNotReadableException e) {

	    return ResponseEntity.badRequest().body(ErrorResp.builder()
	            .errorCode("INVALID_REQUEST_FORMAT")
	            .message("請求資料格式有誤")
	            .build());
	}
	
	@ExceptionHandler(MemberException.class)
	public ResponseEntity<ErrorResp> handleMemberException(MemberException e){
		return ResponseEntity
				.status(e.getHttpStatus())
				.body(ErrorResp.builder()
						.errorCode(e.getErrorCode())
						.message(e.getMessage())
						.build());
	}
	
	@ExceptionHandler(AdminAccountException.class)
	public ResponseEntity<ErrorResp> handleAdminAccountException(AdminAccountException e){
		return ResponseEntity
				.status(e.getHttpStatus())
				.body(ErrorResp.builder()
						.errorCode(e.getErrorCode())
						.message(e.getMessage())
						.build());
	}
	
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResp> handleAuthenticationException(AuthenticationException e){
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResp.builder()
						.errorCode("AUTH_FAILED")
						.message("帳號或密碼錯誤")
						.build());
	}
	
}
