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

/**
 * 訂單模組的商業邏輯與交易服務。
 * 協調會員、購物車、訂單及其項目 Repository，處理結帳、查詢、狀態更新與刪除流程。
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final MemberRepository memberRepository;

	/**
	 * 嘗試將會員目前啟用中的購物車轉為訂單。
	 * 現階段完成鎖定與前置檢查後，會停止建立缺少商品資料的訂單。
	 *
	 * @param memberId 要結帳的會員識別碼
	 * @return 商品資料完整並完成結帳後的訂單資料
	 * @throws CheckoutNotAvailableException 商品資料尚未能完整取得時
	 */
	public OrderResponse checkoutCart(Long memberId) {
		// 先確認會員存在，再鎖定其 ACTIVE 購物車進行結帳檢查。
		findMember(memberId);
		// 悲觀鎖可避免同一購物車被兩個請求同時重複結帳。
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
		 * 商品模組完成前無法取得可信的商品名稱、價格與庫存，因此在這裡停止建立訂單，
		 * 避免寫入金額不完整的 Order、OrderItem，或過早將購物車標記為已結帳。
		 */
		throw new CheckoutNotAvailableException();
	}

	/**
	 * 查詢會員擁有的單筆訂單與訂單項目。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要查詢的訂單識別碼
	 * @return 完整訂單資料
	 */
	@Transactional(Transactional.TxType.SUPPORTS)
	public OrderResponse getOrder(Long memberId, Long orderId) {
		findMember(memberId);
		Order order = findOwnedOrder(memberId, orderId);
		return toResponse(
				order,
				orderItemRepository.findByOrderOrderId(orderId)
		);
	}

	/**
	 * 查詢會員的全部訂單，並依建立時間由新到舊組成回應清單。
	 *
	 * @param memberId 要查詢訂單的會員識別碼
	 * @return 會員的訂單清單
	 */
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

	/**
	 * 更新會員指定訂單的處理狀態。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要更新的訂單識別碼
	 * @param request 新的訂單狀態
	 * @return 更新後的訂單資料
	 */
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

	/**
	 * 更新會員指定訂單的付款狀態。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要更新的訂單識別碼
	 * @param request 新的付款狀態
	 * @return 更新後的訂單資料
	 */
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

	/**
	 * 刪除會員擁有的訂單，並先移除其訂單項目。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要刪除的訂單識別碼
	 */
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
