package com.gigafix.shipment.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gigafix.order.entity.Order;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.shipment.dto.request.CreateShipmentRequest;
import com.gigafix.shipment.dto.response.ShipmentResponse;
import com.gigafix.shipment.entity.Shipment;
import com.gigafix.shipment.enums.ShippingStatus;
import com.gigafix.shipment.exception.DuplicateShipmentException;
import com.gigafix.shipment.exception.InvalidShipmentOperationException;
import com.gigafix.shipment.exception.InvalidShipmentStatusTransitionException;
import com.gigafix.shipment.exception.ShipmentNotFoundException;
import com.gigafix.shipment.mapper.ShipmentMapper;
import com.gigafix.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;

@Service @Transactional @RequiredArgsConstructor
/** 管理物流紀錄的建立、查詢與合法狀態轉換。 */
public class ShipmentService {
	private final ShipmentRepository shipmentRepository;
	private final OrderRepository orderRepository;
	private final ShipmentMapper shipmentMapper;

	/** 以請求中的收件快照，為會員自己的訂單建立唯一物流紀錄。 */
	public ShipmentResponse createShipment(Long memberId,Long orderId,CreateShipmentRequest request){
		validateId(memberId,"memberId");validateId(orderId,"orderId");validateRequest(request);
		Order order=orderRepository.findByOrderIdAndMemberId(orderId,memberId).orElseThrow(()->new ShipmentNotFoundException("找不到訂單"));
		if(shipmentRepository.existsByOrderOrderId(orderId))throw new DuplicateShipmentException(orderId);
		if(order.getStatus()!=Order.OrderStatus.PENDING&&order.getStatus()!=Order.OrderStatus.PROCESSING)throw new InvalidShipmentOperationException("只有待處理或處理中訂單可建立物流紀錄");
		Shipment shipment=Shipment.builder().order(order).receiverName(request.receiverName()).receiverPhone(request.receiverPhone())
				.receiverAddress(request.receiverAddress()).shippingMethod(request.shippingMethod()).shippingStatus(ShippingStatus.PREPARING).build();
		return shipmentMapper.toResponse(shipmentRepository.save(shipment));
	}

	@Transactional(readOnly=true)
	/** 查詢會員自己的訂單物流紀錄，避免跨會員存取。 */
	public ShipmentResponse getShipment(Long memberId,Long orderId){validateId(memberId,"memberId");validateId(orderId,"orderId");return shipmentMapper.toResponse(shipmentRepository.findByOrderOrderIdAndOrderMemberId(orderId,memberId).orElseThrow(()->new ShipmentNotFoundException("找不到物流紀錄")));}

	/** 將備貨中物流標記出貨；此內部流程不由一般會員端點直接呼叫。 */
	public ShipmentResponse markShipped(Long shipmentId,String trackingNumber){validateId(shipmentId,"shipmentId");if(trackingNumber==null||trackingNumber.isBlank())throw new IllegalArgumentException("trackingNumber 不得空白");Shipment s=find(shipmentId);transition(s,ShippingStatus.PREPARING,ShippingStatus.SHIPPED);s.setTrackingNumber(trackingNumber);s.setShippedAt(LocalDateTime.now());return shipmentMapper.toResponse(shipmentRepository.save(s));}
	/** 將已出貨物流標記送達；此內部流程不由一般會員端點直接呼叫。 */
	public ShipmentResponse markDelivered(Long shipmentId){validateId(shipmentId,"shipmentId");Shipment s=find(shipmentId);transition(s,ShippingStatus.SHIPPED,ShippingStatus.DELIVERED);s.setDeliveredAt(LocalDateTime.now());return shipmentMapper.toResponse(shipmentRepository.save(s));}
	/** 只允許取消備貨中的物流紀錄，並保留收件快照。 */
	public ShipmentResponse cancelPreparingShipment(Long shipmentId){validateId(shipmentId,"shipmentId");Shipment s=find(shipmentId);transition(s,ShippingStatus.PREPARING,ShippingStatus.CANCELLED);return shipmentMapper.toResponse(shipmentRepository.save(s));}

	private void transition(Shipment s,ShippingStatus from,ShippingStatus to){if(s.getShippingStatus()!=from)throw new InvalidShipmentStatusTransitionException(s.getShippingStatus(),to);s.setShippingStatus(to);}
	private Shipment find(Long id){return shipmentRepository.findById(id).orElseThrow(()->new ShipmentNotFoundException("找不到物流紀錄"));}
	private void validateRequest(CreateShipmentRequest r){if(r==null)throw new IllegalArgumentException("request 不得為 null");if(r.receiverName()==null||r.receiverName().isBlank()||r.receiverPhone()==null||r.receiverPhone().isBlank()||r.receiverAddress()==null||r.receiverAddress().isBlank()||r.shippingMethod()==null)throw new IllegalArgumentException("收件資料與 shippingMethod 不得空白");}
	private void validateId(Long id,String name){if(id==null||id<=0)throw new IllegalArgumentException(name+" 必須大於 0");}
}
