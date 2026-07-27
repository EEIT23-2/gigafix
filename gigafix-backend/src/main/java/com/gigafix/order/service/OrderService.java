package com.gigafix.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.order.dto.request.CreateOrderItemRequest;
import com.gigafix.order.dto.request.CreateOrderRequest;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderItemResponse;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.exception.InvalidOrderException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;

	public OrderResponse createOrder(CreateOrderRequest request) {
		if (request.items() == null || request.items().isEmpty()) {
			throw new InvalidOrderException("訂單明細不可為空");
		}

		List<BigDecimal> subtotals = new ArrayList<>();
		BigDecimal itemsTotal = BigDecimal.ZERO;

		for (CreateOrderItemRequest item : request.items()) {
			if (item.quantity() == null || item.quantity() <= 0) {
				throw new InvalidOrderException("商品數量必須大於 0");
			}
			if (item.unitPrice() == null
					|| item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
				throw new InvalidOrderException("商品單價不可小於 0");
			}

			BigDecimal subtotal = item.unitPrice()
					.multiply(BigDecimal.valueOf(item.quantity()));
			subtotals.add(subtotal);
			itemsTotal = itemsTotal.add(subtotal);
		}

		BigDecimal totalAmount = itemsTotal
				.add(request.shippingFee())
				.subtract(request.discountAmount());
		if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidOrderException("訂單總金額不可小於 0");
		}

		Order order = Order.builder()
				.userId(request.userId())
				.receiverName(request.receiverName())
				.receiverPhone(request.receiverPhone())
				.shippingAddress(request.shippingAddress())
				.shippingFee(request.shippingFee())
				.discountAmount(request.discountAmount())
				.remark(request.remark())
				.totalAmount(totalAmount)
				.build();
		Order savedOrder = orderRepository.save(order);

		List<OrderItem> orderItems = new ArrayList<>();
		for (int index = 0; index < request.items().size(); index++) {
			CreateOrderItemRequest item = request.items().get(index);
			orderItems.add(OrderItem.builder()
					.orderId(savedOrder.getOrderId())
					.productId(item.productId())
					.productName(item.productName())
					.unitPrice(item.unitPrice())
					.quantity(item.quantity())
					.subtotal(subtotals.get(index))
					.build());
		}

		List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);
		return toResponse(savedOrder, savedItems);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public OrderResponse getOrder(Long orderId) {
		validateId(orderId, "orderId");

		Order order = findOrder(orderId);
		List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
		return toResponse(order, items);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<OrderResponse> getOrdersByUser(Long userId) {
		validateId(userId, "userId");

		return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(order -> toResponse(
						order,
						orderItemRepository.findByOrderId(order.getOrderId())
				))
				.toList();
	}

	public OrderResponse updateOrderStatus(
			Long orderId,
			UpdateOrderStatusRequest request
	) {
		Order order = findOrder(orderId);
		if (request.status() == null) {
			throw new InvalidOrderException("訂單狀態不可為 null");
		}

		order.setStatus(request.status());
		Order savedOrder = orderRepository.save(order);
		List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
		return toResponse(savedOrder, items);
	}

	public OrderResponse updatePaymentStatus(
			Long orderId,
			UpdatePaymentStatusRequest request
	) {
		Order order = findOrder(orderId);
		if (request.paymentStatus() == null) {
			throw new InvalidOrderException("付款狀態不可為 null");
		}

		order.setPaymentStatus(request.paymentStatus());
		Order savedOrder = orderRepository.save(order);
		List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
		return toResponse(savedOrder, items);
	}

	public void deleteOrder(Long orderId) {
		Order order = findOrder(orderId);
		orderItemRepository.deleteByOrderId(orderId);
		orderRepository.delete(order);
	}

	private Order findOrder(Long orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	private void validateId(Long id, String fieldName) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(fieldName + " 必須大於 0");
		}
	}

	private OrderItemResponse toItemResponse(OrderItem item) {
		return new OrderItemResponse(
				item.getOrderItemId(),
				item.getProductId(),
				item.getProductName(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getSubtotal(),
				item.getCreatedAt(),
				item.getUpdatedAt()
		);
	}

	private OrderResponse toResponse(
			Order order,
			List<OrderItem> items
	) {
		return new OrderResponse(
				order.getOrderId(),
				order.getUserId(),
				order.getOrderDate(),
				order.getTotalAmount(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getReceiverName(),
				order.getReceiverPhone(),
				order.getShippingAddress(),
				order.getShippingFee(),
				order.getDiscountAmount(),
				order.getRemark(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				items.stream().map(this::toItemResponse).toList()
		);
	}
}
