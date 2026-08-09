package com.gigafix.shipment.exception;
import com.gigafix.shipment.enums.ShippingStatus;
/** 物流紀錄嘗試進入不合法狀態時拋出。 */
public class InvalidShipmentStatusTransitionException extends RuntimeException { public InvalidShipmentStatusTransitionException(ShippingStatus from,ShippingStatus to){super("不允許將物流狀態由 "+from+" 變更為 "+to);} }
