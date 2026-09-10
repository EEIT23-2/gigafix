package com.gigafix.order.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gigafix.common.dto.ErrorResp;
// 處理購物車和訂單相關的異常
@RestControllerAdvice(basePackages = {
        "com.gigafix.cart.controller",
        "com.gigafix.order.controller"
})
// 優先處理購物車和訂單相關的異常
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CartOrderExceptionHandler {
    // 處理參數異常
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResp> handleIllegalArgument(
            IllegalArgumentException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResp.builder()
                        .errorCode("CART_ORDER_BAD_REQUEST")
                        .message(exception.getMessage())
                        .build());
    }
    // 處理狀態異常
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResp> handleIllegalState(
            IllegalStateException exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResp.builder()
                        .errorCode("CART_ORDER_CONFLICT")
                        .message(exception.getMessage())
                        .build());
    }
}