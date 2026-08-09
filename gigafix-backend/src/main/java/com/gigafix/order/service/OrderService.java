package com.gigafix.order.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.CheckoutNotAvailableException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.exception.InvalidOrderException;
import com.gigafix.order.exception.OrderMemberNotFoundException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.mapper.OrderMapper;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 訂單查詢、狀態轉換與未來 checkout 的交易協調服務。 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final MemberRepository memberRepository;
	private final OrderMapper orderMapper;
	private final OrderStatusPolicy orderStatusPolicy;

	/**
	 * 驗證 checkout 前置條件，但在 Product 契約完成前不建立訂單或改變購物車。
	 */
	public OrderResponse checkoutCart(Long memberId) {
		findMember(memberId);
		Cart cart = cartRepository.findForCheckoutByMemberIdAndStatus(
				memberId,
				Cart.CartStatus.ACTIVE
		).orElseThrow(() -> new CartNotFoundException(memberId));
		List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());
		if (cartItems.isEmpty()) {
			throw new EmptyCartException(cart.getCartId());
		}

		throw new CheckoutNotAvailableException();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	/** 查詢會員自己的單筆訂單與下單快照。 */
	public OrderResponse getOrder(Long memberId, Long orderId) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		return orderMapper.toResponse(
				order,
				orderItemRepository.findByOrderOrderId(orderId)
		);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	/** 依建立時間列出會員自己的訂單。 */
	public List<OrderResponse> getOrdersByMember(Long memberId) {
		findMember(memberId);
		List<Order> orders = orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
		if (orders.isEmpty()) {
			return List.of();
		}

		List<Long> orderIds = orders.stream().map(Order::getOrderId).toList();
		Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository
				.findAllByOrderOrderIdIn(orderIds)
				.stream()
				.collect(Collectors.groupingBy(item -> item.getOrder().getOrderId()));

		return orders.stream()
				.map(order -> orderMapper.toResponse(
						order,
						itemsByOrderId.getOrDefault(order.getOrderId(), List.of())
				))
				.toList();
	}

	/**
	 * 套用集中式狀態規則。此方法目前僅供訂單內部流程與測試使用，未公開為會員 API。
	 */
	public OrderResponse updateOrderStatus(
			Long memberId,
			Long orderId,
			UpdateOrderStatusRequest request
	) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		if (request == null || request.status() == null) {
			throw new InvalidOrderException("訂單狀態不得為 null");
		}
		orderStatusPolicy.validateTransition(order.getStatus(), request.status());
		order.setStatus(request.status());
		return orderMapper.toResponse(
				orderRepository.save(order),
				orderItemRepository.findByOrderOrderId(orderId)
		);
	}

	/**
	 * 取消會員自己的待處理訂單；保留訂單與 snapshot items，不處理 Product release。
	 */
	public OrderResponse cancelOrder(Long memberId, Long orderId) {
		return updateOrderStatus(
				memberId,
				orderId,
				new UpdateOrderStatusRequest(Order.OrderStatus.CANCELLED)
		);
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
}
