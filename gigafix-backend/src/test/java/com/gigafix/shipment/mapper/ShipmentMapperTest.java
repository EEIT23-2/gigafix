package com.gigafix.shipment.mapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.gigafix.order.entity.Order;
import com.gigafix.shipment.entity.Shipment;
import com.gigafix.shipment.enums.ShippingMethod;
import com.gigafix.shipment.enums.ShippingStatus;
import com.gigafix.order.enums.OrderType;
/** 驗證物流回應映射保留收件快照且不暴露訂單 Entity。 */
class ShipmentMapperTest { @Test void mapsSnapshotWithoutReturningOrderEntity(){Shipment s=Shipment.builder().shipmentId(1L).order(Order.builder().orderId(2L).orderType(OrderType.REPAIR).build()).receiverName("王小明").receiverPhone("0912").receiverAddress("台北市").shippingMethod(ShippingMethod.HOME_DELIVERY).shippingStatus(ShippingStatus.PREPARING).build();var r=new ShipmentMapper().toResponse(s);assertEquals(2L,r.orderId());assertEquals(OrderType.REPAIR,r.orderType());assertEquals("台北市",r.receiverAddress());} }
