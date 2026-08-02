package com.gigafix.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.order.entity.Order;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.payment.dto.request.CreatePaymentRequest;
import com.gigafix.payment.dto.response.PaymentResponse;
import com.gigafix.payment.entity.Payment;
import com.gigafix.payment.enums.PaymentRecordStatus;
import com.gigafix.payment.exception.DuplicatePaymentException;
import com.gigafix.payment.exception.InvalidPaymentOperationException;
import com.gigafix.payment.exception.InvalidPaymentStatusTransitionException;
import com.gigafix.payment.exception.PaymentNotFoundException;
import com.gigafix.payment.mapper.PaymentMapper;
import com.gigafix.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
/** 管理付款紀錄的建立、查詢與合法狀態轉換。 */
public class PaymentService {
	private final PaymentRepository paymentRepository;
	private final OrderRepository orderRepository;
	private final PaymentMapper paymentMapper;

	/** 以訂單可信總額建立唯一付款紀錄，並驗證訂單屬於指定會員。 */
	public PaymentResponse createPayment(Long memberId, Long orderId, CreatePaymentRequest request) {
		validateId(memberId, "memberId");
		validateId(orderId, "orderId");
		if (request == null || request.paymentMethod() == null) {
			throw new IllegalArgumentException("paymentMethod 不得為 null");
		}
		Order order = findOwnedOrder(memberId, orderId);
		if (paymentRepository.existsByOrderOrderId(orderId)) throw new DuplicatePaymentException(orderId);
		if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.COMPLETED) {
			throw new InvalidPaymentOperationException("此訂單狀態不可建立付款紀錄");
		}
		if (order.getPaymentStatus() != Order.PaymentStatus.UNPAID) {
			throw new InvalidPaymentOperationException("只有未付款訂單可建立付款紀錄");
		}
		if (order.getTotalAmount() == null) throw new InvalidPaymentOperationException("訂單總金額不存在");
		Payment payment = Payment.builder().order(order).paymentMethod(request.paymentMethod())
				.paymentStatus(PaymentRecordStatus.PENDING).amount(order.getTotalAmount()).build();
		return paymentMapper.toResponse(paymentRepository.save(payment));
	}

	@Transactional(readOnly = true)
	/** 查詢會員自己的訂單付款紀錄，避免跨會員存取。 */
	public PaymentResponse getPayment(Long memberId, Long orderId) {
		validateId(memberId, "memberId");
		validateId(orderId, "orderId");
		return paymentMapper.toResponse(paymentRepository
				.findByOrderOrderIdAndOrderMemberId(orderId, memberId)
				.orElseThrow(() -> new PaymentNotFoundException("找不到付款紀錄")));
	}

	/** 將待付款紀錄標記成功；此內部流程不由一般會員端點直接呼叫。 */
	public PaymentResponse markPaymentPaid(Long paymentId, String transactionId) {
		validateId(paymentId, "paymentId");
		if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("transactionId 不得空白");
		Payment payment = findPayment(paymentId);
		if (payment.getPaymentStatus() == PaymentRecordStatus.PAID) {
			if (transactionId.equals(payment.getTransactionId())) return paymentMapper.toResponse(payment);
			throw new InvalidPaymentOperationException("已付款紀錄不可改用其他 transactionId");
		}
		transition(payment, PaymentRecordStatus.PENDING, PaymentRecordStatus.PAID);
		payment.setTransactionId(transactionId);
		payment.setPaidAt(LocalDateTime.now());
		payment.getOrder().setPaymentStatus(Order.PaymentStatus.PAID);
		// TODO: Product 契約完成後，在付款成功時將 RESERVED 商品轉為 SOLD。
		orderRepository.save(payment.getOrder());
		return paymentMapper.toResponse(paymentRepository.save(payment));
	}

	/** 將待付款紀錄標記失敗，並同步訂單付款摘要。 */
	public PaymentResponse markPaymentFailed(Long paymentId) {
		Payment payment = findPaymentValidated(paymentId);
		transition(payment, PaymentRecordStatus.PENDING, PaymentRecordStatus.PAYMENT_FAILED);
		payment.getOrder().setPaymentStatus(Order.PaymentStatus.PAYMENT_FAILED);
		// TODO: Product 契約完成後，在付款失敗時釋放 RESERVED 商品。
		orderRepository.save(payment.getOrder());
		return paymentMapper.toResponse(paymentRepository.save(payment));
	}

	/** 將已付款紀錄標記退款，並同步訂單付款摘要。 */
	public PaymentResponse markPaymentRefunded(Long paymentId) {
		Payment payment = findPaymentValidated(paymentId);
		transition(payment, PaymentRecordStatus.PAID, PaymentRecordStatus.REFUNDED);
		payment.getOrder().setPaymentStatus(Order.PaymentStatus.REFUNDED);
		orderRepository.save(payment.getOrder());
		return paymentMapper.toResponse(paymentRepository.save(payment));
	}

	/** 只允許取消待付款紀錄；此內部流程不由一般會員端點直接呼叫。 */
	public PaymentResponse cancelPendingPayment(Long paymentId) {
		Payment payment = findPaymentValidated(paymentId);
		transition(payment, PaymentRecordStatus.PENDING, PaymentRecordStatus.CANCELLED);
		// TODO: Product 契約完成後，在取消付款時釋放 RESERVED 商品。
		return paymentMapper.toResponse(paymentRepository.save(payment));
	}

	/** 將付款失敗紀錄重設為待付款，供同一筆付款重新嘗試。 */
	public PaymentResponse retryFailedPayment(Long paymentId) {
		Payment payment = findPaymentValidated(paymentId);
		transition(payment, PaymentRecordStatus.PAYMENT_FAILED, PaymentRecordStatus.PENDING);
		payment.setTransactionId(null);
		payment.setPaidAt(null);
		payment.getOrder().setPaymentStatus(Order.PaymentStatus.UNPAID);
		orderRepository.save(payment.getOrder());
		return paymentMapper.toResponse(paymentRepository.save(payment));
	}

	private void transition(Payment payment, PaymentRecordStatus from, PaymentRecordStatus to) {
		if (payment.getPaymentStatus() != from) throw new InvalidPaymentStatusTransitionException(payment.getPaymentStatus(), to);
		payment.setPaymentStatus(to);
	}

	private Payment findPaymentValidated(Long id) { validateId(id, "paymentId"); return findPayment(id); }
	private Payment findPayment(Long id) { return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException("找不到付款紀錄")); }
	private Order findOwnedOrder(Long memberId, Long orderId) { return orderRepository.findByOrderIdAndMemberId(orderId, memberId).orElseThrow(() -> new PaymentNotFoundException("找不到訂單")); }
	private void validateId(Long id, String name) { if (id == null || id <= 0) throw new IllegalArgumentException(name + " 必須大於 0"); }
}
