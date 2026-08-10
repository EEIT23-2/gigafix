package com.gigafix.common.exception;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.gigafix.common.dto.ErrorResp;
import com.gigafix.common.dto.FieldErrorDetail;

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
				.toList();
		
		return ResponseEntity.badRequest().body(ErrorResp.builder()
										.errorCode("VALIDATION_FAILED")
										.message("參數驗證失敗")
										.errors(fieldErrors).build());
	}
	/*ConstraintViolationException是@Valid使用在非controller時(非controller必須在class上方加上@Validated)，如果驗證失敗會throw這個錯誤
	 * 但目前使用@Validated的Bean是JwtUtils，而使用@Valid檢查的method會是工程師自己發出去的內容，如果報錯表示程式有漏洞，不該使用ExceptionHandler catch起來*/
	
	
}
