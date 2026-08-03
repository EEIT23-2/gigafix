package com.gigafix.shipment.exception;
/** 找不到會員可存取的訂單或物流紀錄時拋出。 */
public class ShipmentNotFoundException extends RuntimeException { public ShipmentNotFoundException(String message){super(message);} }
