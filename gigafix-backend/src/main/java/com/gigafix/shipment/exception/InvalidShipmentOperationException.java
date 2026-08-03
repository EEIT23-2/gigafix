package com.gigafix.shipment.exception;
/** 訂單或物流狀態不允許目前操作時拋出。 */
public class InvalidShipmentOperationException extends RuntimeException { public InvalidShipmentOperationException(String message){super(message);} }
