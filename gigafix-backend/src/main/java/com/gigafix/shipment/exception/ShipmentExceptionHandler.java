package com.gigafix.shipment.exception;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages="com.gigafix.shipment.controller")
/** 將物流模組例外轉為一致且不洩漏內部資訊的 API 錯誤。 */
public class ShipmentExceptionHandler {
	@ExceptionHandler(ShipmentNotFoundException.class)
	ResponseEntity<ShipmentErrorResponse> notFound(RuntimeException ex,HttpServletRequest req){return build(HttpStatus.NOT_FOUND,ex.getMessage(),req);}
	@ExceptionHandler({DuplicateShipmentException.class,InvalidShipmentOperationException.class,InvalidShipmentStatusTransitionException.class})
	ResponseEntity<ShipmentErrorResponse> conflict(RuntimeException ex,HttpServletRequest req){return build(HttpStatus.CONFLICT,ex.getMessage(),req);}
	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ShipmentErrorResponse> badRequest(RuntimeException ex,HttpServletRequest req){return build(HttpStatus.BAD_REQUEST,ex.getMessage(),req);}
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ShipmentErrorResponse> validation(MethodArgumentNotValidException ex,HttpServletRequest req){String m=ex.getBindingResult().getFieldErrors().stream().findFirst().map(e->e.getField()+": "+e.getDefaultMessage()).orElse("Validation failed");return build(HttpStatus.BAD_REQUEST,m,req);}
	private ResponseEntity<ShipmentErrorResponse> build(HttpStatus s,String m,HttpServletRequest r){return ResponseEntity.status(s).body(new ShipmentErrorResponse(LocalDateTime.now(),s.value(),s.getReasonPhrase(),m,r.getRequestURI()));}
}
