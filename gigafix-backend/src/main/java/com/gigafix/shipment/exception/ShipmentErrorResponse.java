package com.gigafix.shipment.exception;
import java.time.LocalDateTime;
/** 統一物流 API 的錯誤回應格式。 */
public record ShipmentErrorResponse(LocalDateTime timestamp,int status,String error,String message,String path) {}
