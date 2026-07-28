package com.gigafix.order.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.CheckoutNotAvailableException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderItemResponse;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.exception.InvalidOrderException;
import com.gigafix.order.exception.OrderMemberNotFoundException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.user.entity.Member;
import com.gigafix.user.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final MemberRepository memberRepository;

	public OrderResponse checkoutCart(Long memberId) {
		findMember(memberId);
		Cart cart = cartRepository.findForCheckoutByMemberIdAndStatus(
				memberId,
				Cart.CartStatus.ACTIVE
		).orElseThrow(() -> new CartNotFoundException(memberId));
		List<CartItem> cartItems = cartItemRepository.findByCartCartId(
				cart.getCartId()
		);
		if (cartItems.isEmpty()) {
			throw new EmptyCartException(cart.getCartId());
		}

		/*
		 * TODO(product-integration): resolve each productId from the product
		 * module, reject unavailable products, obtain the authoritative name and
		 * price, calculate totals, lock and deduct stock, and add concurrency
		 * control. Only then create Order/OrderItem and mark this locked Cart as
		 * CHECKED_OUT in this transaction.
		 */
		throw new CheckoutNotAvailableException();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public OrderResponse getOrder(Long memberId, Long orderId) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		return toResponse(
				order,
				orderItemRepository.findByOrderOrderId(orderId)
		);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<OrderResponse> getOrdersByMember(Long memberId) {
		findMember(memberId);
		return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
				.stream()
				.map(order -> toResponse(
						order,
						orderItemRepository.findByOrderOrderId(
								order.getOrderId()
						)
				))
				.toList();
	}

	public OrderResponse updateOrderStatus(
			Long memberId,
			Long orderId,
			UpdateOrderStatusRequest request
	) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		if (request.status() == null) {
			throw new InvalidOrderException("訂單狀態不可為 null");
		}
		order.setStatus(request.status());
		return toResponse(
				orderRepository.save(order),
				orderItemRepository.findByOrderOrderId(orderId)
		);
	}

	public OrderResponse updatePaymentStatus(
			Long memberId,
			Long orderId,
			UpdatePaymentStatusRequest request
	) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		if (request.paymentStatus() == null) {
			throw new InvalidOrderException("付款狀態不可為 null");
		}
		order.setPaymentStatus(request.paymentStatus());
		return toResponse(
				orderRepository.save(order),
				orderItemRepository.findByOrderOrderId(orderId)
		);
	}

	public void deleteOrder(Long memberId, Long orderId) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		orderItemRepository.deleteByOrderOrderId(orderId);
		orderRepository.delete(order);
	}

	private Member findMember(Long memberId) {
		validateId(memberId, "memberId");
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new OrderMemberNotFoundException(memberId));
	}

	private Order findOwnedOrder(Long memberId, Long orderId) {
		validateId(orderId, "orderId");
		return orderRepository.findByOrderIdAndMemberId(orderId, memberId)
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

	private OrderResponse toResponse(Order order, List<OrderItem> items) {
		return new OrderResponse(
				order.getOrderId(),
				order.getMember().getId(),
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
