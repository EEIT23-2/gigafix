package com.gigafix.shipment.exception;
/** 同一張訂單重複建立物流紀錄時拋出。 */
public class DuplicateShipmentException extends RuntimeException { public DuplicateShipmentException(Long orderId){super("訂單已有物流紀錄，orderId："+orderId);} }
