package com.gigafix.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.gigafix.member.entity.Member;
import com.gigafix.order.entity.Order;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.order.enums.OrderType;
import com.gigafix.payment.dto.request.CreatePaymentRequest;
import com.gigafix.payment.entity.Payment;
import com.gigafix.payment.enums.PaymentMethod;
import com.gigafix.payment.enums.PaymentRecordStatus;
import com.gigafix.payment.exception.DuplicatePaymentException;
import com.gigafix.payment.exception.InvalidPaymentOperationException;
import com.gigafix.payment.exception.InvalidPaymentStatusTransitionException;
import com.gigafix.payment.exception.PaymentNotFoundException;
import com.gigafix.payment.mapper.PaymentMapper;
import com.gigafix.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
/** 驗證付款建立、狀態轉換與訂單付款摘要同步。 */
class PaymentServiceTest {
	@Mock PaymentRepository paymentRepository;
	@Mock OrderRepository orderRepository;
	PaymentService service;
	Order order;

	@BeforeEach void setUp(){service=new PaymentService(paymentRepository,orderRepository,new PaymentMapper());order=Order.builder().orderId(10L).member(Member.builder().id(1L).build()).orderType(OrderType.GENERAL).totalAmount(new BigDecimal("1200.00")).status(Order.OrderStatus.PENDING).paymentStatus(Order.PaymentStatus.UNPAID).build();}

	@Test void createsPendingPaymentUsingTrustedOrderAmount(){ownedOrder();when(paymentRepository.existsByOrderOrderId(10L)).thenReturn(false);when(paymentRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i->{Payment p=i.getArgument(0);p.setPaymentId(20L);return p;});var response=service.createPayment(1L,10L,new CreatePaymentRequest(PaymentMethod.CREDIT_CARD));assertEquals(new BigDecimal("1200.00"),response.amount());assertEquals(PaymentRecordStatus.PENDING,response.paymentStatus());}
	@Test void rejectsDuplicatePayment(){ownedOrder();when(paymentRepository.existsByOrderOrderId(10L)).thenReturn(true);assertThrows(DuplicatePaymentException.class,()->service.createPayment(1L,10L,new CreatePaymentRequest(PaymentMethod.CREDIT_CARD)));verify(paymentRepository,never()).save(org.mockito.ArgumentMatchers.any());}
	@Test void missingOwnedOrderIsNotFound(){when(orderRepository.findByOrderIdAndMemberId(10L,1L)).thenReturn(Optional.empty());assertThrows(PaymentNotFoundException.class,()->service.createPayment(1L,10L,new CreatePaymentRequest(PaymentMethod.CREDIT_CARD)));}
	@Test void cancelledAndCompletedOrdersAreRejected(){for(Order.OrderStatus status:new Order.OrderStatus[]{Order.OrderStatus.CANCELLED,Order.OrderStatus.COMPLETED}){order.setStatus(status);ownedOrder();assertThrows(InvalidPaymentOperationException.class,()->service.createPayment(1L,10L,new CreatePaymentRequest(PaymentMethod.CREDIT_CARD)));}}
	@Test void alreadyPaidOrderIsRejected(){order.setPaymentStatus(Order.PaymentStatus.PAID);ownedOrder();assertThrows(InvalidPaymentOperationException.class,()->service.createPayment(1L,10L,new CreatePaymentRequest(PaymentMethod.CREDIT_CARD)));}
	@Test void getPaymentUsesOwnershipQuery(){Payment p=payment(PaymentRecordStatus.PENDING);when(paymentRepository.findByOrderOrderIdAndOrderMemberId(10L,1L)).thenReturn(Optional.of(p));assertEquals(20L,service.getPayment(1L,10L).paymentId());}
	@Test void markPaidUpdatesPaymentAndOrder(){Payment p=payment(PaymentRecordStatus.PENDING);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));when(paymentRepository.save(p)).thenReturn(p);var response=service.markPaymentPaid(20L,"TX-1");assertEquals(PaymentRecordStatus.PAID,response.paymentStatus());assertEquals(Order.PaymentStatus.PAID,order.getPaymentStatus());assertNotNull(response.paidAt());verify(orderRepository).save(order);}
	@Test void samePaidTransactionIsIdempotentButDifferentIsRejected(){Payment p=payment(PaymentRecordStatus.PAID);p.setTransactionId("TX-1");when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));assertEquals(PaymentRecordStatus.PAID,service.markPaymentPaid(20L,"TX-1").paymentStatus());assertThrows(InvalidPaymentOperationException.class,()->service.markPaymentPaid(20L,"TX-2"));}
	@Test void failedCannotBecomePaid(){Payment p=payment(PaymentRecordStatus.PAYMENT_FAILED);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));assertThrows(InvalidPaymentStatusTransitionException.class,()->service.markPaymentPaid(20L,"TX-1"));}
	@Test void failedRefundedAndCancelledTransitionsUpdateExpectedState(){Payment failed=payment(PaymentRecordStatus.PENDING);when(paymentRepository.findById(20L)).thenReturn(Optional.of(failed));when(paymentRepository.save(failed)).thenReturn(failed);assertEquals(PaymentRecordStatus.PAYMENT_FAILED,service.markPaymentFailed(20L).paymentStatus());assertEquals(Order.PaymentStatus.PAYMENT_FAILED,order.getPaymentStatus());Payment refund=payment(PaymentRecordStatus.PAID);when(paymentRepository.findById(21L)).thenReturn(Optional.of(refund));refund.setPaymentId(21L);when(paymentRepository.save(refund)).thenReturn(refund);assertEquals(PaymentRecordStatus.REFUNDED,service.markPaymentRefunded(21L).paymentStatus());Payment cancel=payment(PaymentRecordStatus.PENDING);cancel.setPaymentId(22L);when(paymentRepository.findById(22L)).thenReturn(Optional.of(cancel));when(paymentRepository.save(cancel)).thenReturn(cancel);assertEquals(PaymentRecordStatus.CANCELLED,service.cancelPendingPayment(22L).paymentStatus());}
	@Test void blankTransactionIdIsRejected(){assertThrows(IllegalArgumentException.class,()->service.markPaymentPaid(20L," "));verify(paymentRepository,never()).findById(20L);}
	@Test void paidIdempotencyKeepsPaidAtAndDoesNotSaveAgain(){Payment p=payment(PaymentRecordStatus.PAID);LocalDateTime paidAt=LocalDateTime.of(2026,8,2,12,0);p.setTransactionId("TX-1");p.setPaidAt(paidAt);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));var response=service.markPaymentPaid(20L,"TX-1");assertEquals(paidAt,response.paidAt());verify(paymentRepository,never()).save(p);verify(orderRepository,never()).save(order);}
	@Test void paidCannotBecomeFailed(){Payment p=payment(PaymentRecordStatus.PAID);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));assertThrows(InvalidPaymentStatusTransitionException.class,()->service.markPaymentFailed(20L));}
	@Test void refundedAndCancelledCannotBecomePaid(){for(PaymentRecordStatus status:new PaymentRecordStatus[]{PaymentRecordStatus.REFUNDED,PaymentRecordStatus.CANCELLED}){Payment p=payment(status);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));assertThrows(InvalidPaymentStatusTransitionException.class,()->service.markPaymentPaid(20L,"TX-1"));}}
	@Test void refundSynchronizesOrderPaymentStatus(){Payment p=payment(PaymentRecordStatus.PAID);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));when(paymentRepository.save(p)).thenReturn(p);service.markPaymentRefunded(20L);assertEquals(Order.PaymentStatus.REFUNDED,order.getPaymentStatus());verify(orderRepository).save(order);}
	@Test void cancellingPendingPaymentKeepsOrderUnpaid(){Payment p=payment(PaymentRecordStatus.PENDING);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));when(paymentRepository.save(p)).thenReturn(p);service.cancelPendingPayment(20L);assertEquals(Order.PaymentStatus.UNPAID,order.getPaymentStatus());}
	@Test void failedPaymentCanRetryUsingSameRecord(){Payment p=payment(PaymentRecordStatus.PAYMENT_FAILED);p.setTransactionId("FAILED-TX");p.setPaidAt(LocalDateTime.now());order.setPaymentStatus(Order.PaymentStatus.PAYMENT_FAILED);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));when(paymentRepository.save(p)).thenReturn(p);var response=service.retryFailedPayment(20L);assertEquals(PaymentRecordStatus.PENDING,response.paymentStatus());assertNull(response.transactionId());assertNull(response.paidAt());assertEquals(Order.PaymentStatus.UNPAID,order.getPaymentStatus());verify(orderRepository).save(order);}
	@Test void nonFailedPaymentsCannotRetry(){for(PaymentRecordStatus status:new PaymentRecordStatus[]{PaymentRecordStatus.PENDING,PaymentRecordStatus.PAID,PaymentRecordStatus.REFUNDED,PaymentRecordStatus.CANCELLED}){Payment p=payment(status);when(paymentRepository.findById(20L)).thenReturn(Optional.of(p));assertThrows(InvalidPaymentStatusTransitionException.class,()->service.retryFailedPayment(20L));}}
	@Test void retryRequiresPositivePaymentId(){assertThrows(IllegalArgumentException.class,()->service.retryFailedPayment(0L));verify(paymentRepository,never()).findById(0L);}
	@Test void failedAndRefundedOrdersCannotCreatePayment(){for(Order.PaymentStatus status:new Order.PaymentStatus[]{Order.PaymentStatus.PAYMENT_FAILED,Order.PaymentStatus.REFUNDED}){order.setPaymentStatus(status);ownedOrder();assertThrows(InvalidPaymentOperationException.class,()->service.createPayment(1L,10L,new CreatePaymentRequest(PaymentMethod.CREDIT_CARD)));verify(paymentRepository,never()).save(org.mockito.ArgumentMatchers.any());}}

	private void ownedOrder(){when(orderRepository.findByOrderIdAndMemberId(10L,1L)).thenReturn(Optional.of(order));}
	private Payment payment(PaymentRecordStatus status){return Payment.builder().paymentId(20L).order(order).paymentMethod(PaymentMethod.CREDIT_CARD).paymentStatus(status).amount(order.getTotalAmount()).build();}
}
