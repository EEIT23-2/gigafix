package com.gigafix.payment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.gigafix.payment.dto.request.CreatePaymentRequest;
import com.gigafix.payment.dto.response.PaymentResponse;
import com.gigafix.payment.enums.PaymentMethod;
import com.gigafix.payment.enums.PaymentRecordStatus;
import com.gigafix.payment.exception.DuplicatePaymentException;
import com.gigafix.payment.exception.PaymentExceptionHandler;
import com.gigafix.payment.exception.PaymentNotFoundException;
import com.gigafix.payment.service.PaymentService;
import com.gigafix.order.enums.OrderType;

@WebMvcTest(PaymentController.class) @Import(PaymentExceptionHandler.class) @ActiveProfiles("test")
/** 驗證付款 API 的請求驗證、ownership 錯誤與公開端點範圍。 */
class PaymentControllerTest {
	@Autowired MockMvc mvc; @MockitoBean PaymentService service;
	@Test void postReturnsCreated() throws Exception {var req=new CreatePaymentRequest(PaymentMethod.CREDIT_CARD);when(service.createPayment(1L,10L,req)).thenReturn(response());mvc.perform(post("/api/members/1/orders/10/payment").contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"CREDIT_CARD\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.orderType").value("GENERAL")).andExpect(jsonPath("$.amount").value(1200.00));}
	@Test void getReturnsOk() throws Exception {when(service.getPayment(1L,10L)).thenReturn(response());mvc.perform(get("/api/members/1/orders/10/payment")).andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("PENDING"));}
	@Test void nullMethodReturnsBadRequest() throws Exception {mvc.perform(post("/api/members/1/orders/10/payment").contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":null}")).andExpect(status().isBadRequest());}
	@Test void ownershipReturnsNotFound() throws Exception {when(service.getPayment(1L,10L)).thenThrow(new PaymentNotFoundException("找不到付款紀錄"));mvc.perform(get("/api/members/1/orders/10/payment")).andExpect(status().isNotFound());}
	@Test void duplicateReturnsConflict() throws Exception {var req=new CreatePaymentRequest(PaymentMethod.CREDIT_CARD);when(service.createPayment(1L,10L,req)).thenThrow(new DuplicatePaymentException(10L));mvc.perform(post("/api/members/1/orders/10/payment").contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"CREDIT_CARD\"}")).andExpect(status().isConflict());}
	@Test void internalStatusEndpointsAreNotExposed() throws Exception {mvc.perform(patch("/api/members/1/orders/10/payment/paid")).andExpect(status().isNotFound());mvc.perform(patch("/api/members/1/orders/10/payment/refund")).andExpect(status().isNotFound());}
	private PaymentResponse response(){return new PaymentResponse(20L,10L,OrderType.GENERAL,PaymentMethod.CREDIT_CARD,PaymentRecordStatus.PENDING,null,new BigDecimal("1200.00"),null,null,null);}
}
