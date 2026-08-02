package com.gigafix.payment.mapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.gigafix.order.entity.Order;
import com.gigafix.payment.entity.Payment;
import com.gigafix.payment.enums.PaymentMethod;
import com.gigafix.payment.enums.PaymentRecordStatus;
import com.gigafix.order.enums.OrderType;
/** 驗證付款回應映射不會直接暴露訂單 Entity。 */
class PaymentMapperTest { @Test void mapsFieldsWithoutReturningOrderEntity(){Payment p=Payment.builder().paymentId(1L).order(Order.builder().orderId(2L).orderType(OrderType.REPAIR).build()).paymentMethod(PaymentMethod.BANK_TRANSFER).paymentStatus(PaymentRecordStatus.PENDING).amount(new BigDecimal("9.99")).build();var r=new PaymentMapper().toResponse(p);assertEquals(2L,r.orderId());assertEquals(OrderType.REPAIR,r.orderType());assertEquals(new BigDecimal("9.99"),r.amount());} }
