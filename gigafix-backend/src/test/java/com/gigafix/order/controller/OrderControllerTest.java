package com.gigafix.order.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.exception.OrderExceptionHandler;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.service.OrderService;
import com.gigafix.user.exception.MemberNotFoundException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(OrderController.class)
@Import(OrderExceptionHandler.class)
@ActiveProfiles("test")
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrderService orderService;

	@Test
	void postOrderIsNotExposed() throws Exception {
		mockMvc.perform(post("/api/members/1/orders"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void getOrdersUsesMemberPath() throws Exception {
		when(orderService.getOrdersByMember(1L))
				.thenReturn(List.of(response()));

		mockMvc.perform(get("/api/members/1/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].memberId").value(1));
	}

	@Test
	void getAnotherMembersOrderReturnsNotFound() throws Exception {
		when(orderService.getOrder(1L, 300L))
				.thenThrow(new OrderNotFoundException(300L));

		mockMvc.perform(get("/api/members/1/orders/300"))
				.andExpect(status().isNotFound());
	}

	@Test
	void missingMemberReturnsNotFound() throws Exception {
		when(orderService.getOrdersByMember(99L))
				.thenThrow(new MemberNotFoundException(99L));

		mockMvc.perform(get("/api/members/99/orders"))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateStatusUsesMemberOwnership() throws Exception {
		UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(
				Order.OrderStatus.PROCESSING
		);
		when(orderService.updateOrderStatus(1L, 300L, request))
				.thenReturn(response());

		mockMvc.perform(patch("/api/members/1/orders/300/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		verify(orderService).updateOrderStatus(1L, 300L, request);
	}

	@Test
	void updateAnotherMembersPaymentReturnsNotFound() throws Exception {
		UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest(
				Order.PaymentStatus.PAID
		);
		when(orderService.updatePaymentStatus(1L, 300L, request))
				.thenThrow(new OrderNotFoundException(300L));

		mockMvc.perform(patch(
						"/api/members/1/orders/300/payment-status"
				)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteAnotherMembersOrderReturnsNotFound() throws Exception {
		doThrow(new OrderNotFoundException(300L))
				.when(orderService).deleteOrder(1L, 300L);

		mockMvc.perform(delete("/api/members/1/orders/300"))
				.andExpect(status().isNotFound());
	}

	@Test
	void nullOrderStatusIsRejectedByValidation() throws Exception {
		mockMvc.perform(patch("/api/members/1/orders/300/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":null}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nullPaymentStatusIsRejectedByValidation() throws Exception {
		mockMvc.perform(patch(
						"/api/members/1/orders/300/payment-status"
				)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentStatus":null}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void legacyOrderApiIsNotExposed() throws Exception {
		mockMvc.perform(get("/api/orders/user/1"))
				.andExpect(status().isNotFound());
	}

	private OrderResponse response() {
		return new OrderResponse(
				300L,
				1L,
				null,
				null,
				Order.OrderStatus.PENDING,
				Order.PaymentStatus.UNPAID,
				"會員一",
				"0912345678",
				"台北市",
				null,
				null,
				null,
				null,
				null,
				List.of()
		);
	}
}
