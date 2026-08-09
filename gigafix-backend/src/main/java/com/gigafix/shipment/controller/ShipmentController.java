package com.gigafix.shipment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gigafix.shipment.dto.request.CreateShipmentRequest;
import com.gigafix.shipment.dto.response.ShipmentResponse;
import com.gigafix.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/members/{memberId}/orders/{orderId}/shipment") @RequiredArgsConstructor
/** 提供會員建立與查詢自己訂單物流紀錄的 API。 */
public class ShipmentController {
	private final ShipmentService shipmentService;
	/** 建立指定會員訂單的唯一物流紀錄。 */
	@PostMapping public ResponseEntity<ShipmentResponse> create(@PathVariable Long memberId,@PathVariable Long orderId,@Valid @RequestBody CreateShipmentRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.createShipment(memberId,orderId,request));}
	/** 查詢指定會員訂單的物流紀錄。 */
	@GetMapping public ResponseEntity<ShipmentResponse> get(@PathVariable Long memberId,@PathVariable Long orderId){return ResponseEntity.ok(shipmentService.getShipment(memberId,orderId));}
}
