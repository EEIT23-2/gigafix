package com.gigafix.shipment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.gigafix.member.entity.Member;
import com.gigafix.order.entity.Order;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.order.enums.OrderType;
import com.gigafix.shipment.dto.request.CreateShipmentRequest;
import com.gigafix.shipment.entity.Shipment;
import com.gigafix.shipment.enums.ShippingMethod;
import com.gigafix.shipment.enums.ShippingStatus;
import com.gigafix.shipment.exception.DuplicateShipmentException;
import com.gigafix.shipment.exception.InvalidShipmentOperationException;
import com.gigafix.shipment.exception.InvalidShipmentStatusTransitionException;
import com.gigafix.shipment.exception.ShipmentNotFoundException;
import com.gigafix.shipment.mapper.ShipmentMapper;
import com.gigafix.shipment.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
/** 驗證物流建立、收件快照與合法狀態轉換。 */
class ShipmentServiceTest {
	@Mock ShipmentRepository repository; @Mock OrderRepository orderRepository; ShipmentService service; Order order;
	@BeforeEach void setUp(){service=new ShipmentService(repository,orderRepository,new ShipmentMapper());order=Order.builder().orderId(10L).member(Member.builder().id(1L).build()).orderType(OrderType.GENERAL).status(Order.OrderStatus.PENDING).build();}
	@Test void createsPreparingShipmentFromRequestSnapshot(){ownedOrder();when(repository.existsByOrderOrderId(10L)).thenReturn(false);when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i->{Shipment s=i.getArgument(0);s.setShipmentId(30L);return s;});var r=service.createShipment(1L,10L,request());assertEquals(ShippingStatus.PREPARING,r.shippingStatus());assertEquals("台北市",r.receiverAddress());}
	@Test void duplicateShipmentIsRejected(){ownedOrder();when(repository.existsByOrderOrderId(10L)).thenReturn(true);assertThrows(DuplicateShipmentException.class,()->service.createShipment(1L,10L,request()));verify(repository,never()).save(org.mockito.ArgumentMatchers.any());}
	@Test void missingOwnedOrderIsNotFound(){when(orderRepository.findByOrderIdAndMemberId(10L,1L)).thenReturn(Optional.empty());assertThrows(ShipmentNotFoundException.class,()->service.createShipment(1L,10L,request()));}
	@Test void cancelledOrderIsRejected(){order.setStatus(Order.OrderStatus.CANCELLED);ownedOrder();assertThrows(InvalidShipmentOperationException.class,()->service.createShipment(1L,10L,request()));}
	@Test void getUsesOwnershipQuery(){Shipment s=shipment(ShippingStatus.PREPARING);when(repository.findByOrderOrderIdAndOrderMemberId(10L,1L)).thenReturn(Optional.of(s));assertEquals(30L,service.getShipment(1L,10L).shipmentId());}
	@Test void preparingCanShipWithTrackingNumber(){Shipment s=shipment(ShippingStatus.PREPARING);when(repository.findById(30L)).thenReturn(Optional.of(s));when(repository.save(s)).thenReturn(s);var r=service.markShipped(30L,"TRACK-1");assertEquals(ShippingStatus.SHIPPED,r.shippingStatus());assertNotNull(r.shippedAt());assertEquals("TRACK-1",r.trackingNumber());}
	@Test void blankTrackingNumberIsRejected(){assertThrows(IllegalArgumentException.class,()->service.markShipped(30L," "));}
	@Test void shippedCanDeliver(){Shipment s=shipment(ShippingStatus.SHIPPED);when(repository.findById(30L)).thenReturn(Optional.of(s));when(repository.save(s)).thenReturn(s);var r=service.markDelivered(30L);assertEquals(ShippingStatus.DELIVERED,r.shippingStatus());assertNotNull(r.deliveredAt());}
	@Test void preparingCanCancelWithoutDeleting(){Shipment s=shipment(ShippingStatus.PREPARING);when(repository.findById(30L)).thenReturn(Optional.of(s));when(repository.save(s)).thenReturn(s);assertEquals(ShippingStatus.CANCELLED,service.cancelPreparingShipment(30L).shippingStatus());verify(repository,never()).delete(s);}
	@Test void shippedAndDeliveredCannotCancel(){for(ShippingStatus status:new ShippingStatus[]{ShippingStatus.SHIPPED,ShippingStatus.DELIVERED}){Shipment s=shipment(status);when(repository.findById(30L)).thenReturn(Optional.of(s));assertThrows(InvalidShipmentStatusTransitionException.class,()->service.cancelPreparingShipment(30L));}}
	@Test void processingOrderCanCreateShipment(){order.setStatus(Order.OrderStatus.PROCESSING);ownedOrder();when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i->i.getArgument(0));assertEquals(ShippingStatus.PREPARING,service.createShipment(1L,10L,request()).shippingStatus());}
	@Test void shippedCompletedAndCancelledOrdersCannotCreateShipment(){for(Order.OrderStatus status:new Order.OrderStatus[]{Order.OrderStatus.SHIPPED,Order.OrderStatus.COMPLETED,Order.OrderStatus.CANCELLED}){order.setStatus(status);ownedOrder();assertThrows(InvalidShipmentOperationException.class,()->service.createShipment(1L,10L,request()));verify(repository,never()).save(org.mockito.ArgumentMatchers.any());}}
	@Test void preparingCannotBeDeliveredDirectly(){Shipment s=shipment(ShippingStatus.PREPARING);when(repository.findById(30L)).thenReturn(Optional.of(s));assertThrows(InvalidShipmentStatusTransitionException.class,()->service.markDelivered(30L));}
	@Test void deliveredKeepsOriginalShippedAt(){Shipment s=shipment(ShippingStatus.SHIPPED);LocalDateTime shippedAt=LocalDateTime.of(2026,8,2,12,0);s.setShippedAt(shippedAt);when(repository.findById(30L)).thenReturn(Optional.of(s));when(repository.save(s)).thenReturn(s);var response=service.markDelivered(30L);assertEquals(shippedAt,s.getShippedAt());assertNotNull(response.deliveredAt());}
	@Test void cancelledShipmentCannotTransitionAgain(){Shipment s=shipment(ShippingStatus.CANCELLED);when(repository.findById(30L)).thenReturn(Optional.of(s));assertThrows(InvalidShipmentStatusTransitionException.class,()->service.markShipped(30L,"TRACK-1"));assertThrows(InvalidShipmentStatusTransitionException.class,()->service.markDelivered(30L));assertThrows(InvalidShipmentStatusTransitionException.class,()->service.cancelPreparingShipment(30L));}
	private void ownedOrder(){when(orderRepository.findByOrderIdAndMemberId(10L,1L)).thenReturn(Optional.of(order));}
	private CreateShipmentRequest request(){return new CreateShipmentRequest("王小明","0912345678","台北市",ShippingMethod.HOME_DELIVERY);}
	private Shipment shipment(ShippingStatus status){return Shipment.builder().shipmentId(30L).order(order).receiverName("王小明").receiverPhone("0912345678").receiverAddress("台北市").shippingMethod(ShippingMethod.HOME_DELIVERY).shippingStatus(status).build();}
}
