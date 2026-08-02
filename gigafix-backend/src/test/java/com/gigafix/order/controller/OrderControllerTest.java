package com.gigafix.order.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.enums.OrderType;
import com.gigafix.order.exception.InvalidOrderStatusTransitionException;
import com.gigafix.order.exception.OrderExceptionHandler;
import com.gigafix.order.exception.OrderMemberNotFoundException;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.service.OrderService;

/** 驗證訂單查詢與取消 API 的 ownership 及錯誤回應。 */
@WebMvcTest(OrderController.class)
@Import(OrderExceptionHandler.class)
@ActiveProfiles("test")
class OrderControllerTest {

	@Autowired MockMvc mockMvc;
	@MockitoBean OrderService orderService;

	@Test
	void postOrderIsNotExposed() throws Exception {
		mockMvc.perform(post("/api/members/1/orders"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void getOrdersUsesMemberOwnershipPath() throws Exception {
		when(orderService.getOrdersByMember(1L)).thenReturn(List.of(response()));
		mockMvc.perform(get("/api/members/1/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].memberId").value(1))
				.andExpect(jsonPath("$[0].orderType").value("GENERAL"));
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
				.thenThrow(new OrderMemberNotFoundException(99L));
		mockMvc.perform(get("/api/members/99/orders"))
				.andExpect(status().isNotFound());
	}

	@Test
	void cancelOrderReturnsUpdatedSnapshot() throws Exception {
		when(orderService.cancelOrder(1L, 300L)).thenReturn(cancelledResponse());
		mockMvc.perform(patch("/api/members/1/orders/300/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void illegalCancellationReturnsConflict() throws Exception {
		when(orderService.cancelOrder(1L, 300L)).thenThrow(
				new InvalidOrderStatusTransitionException(
						Order.OrderStatus.SHIPPED,
						Order.OrderStatus.CANCELLED
				)
		);
		mockMvc.perform(patch("/api/members/1/orders/300/cancel"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void arbitraryOrderStatusEndpointIsNotExposed() throws Exception {
		mockMvc.perform(patch("/api/members/1/orders/300/status")
					.contentType("application/json")
					.content("{\"status\":\"PROCESSING\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void paymentStatusEndpointIsNotExposed() throws Exception {
		mockMvc.perform(patch("/api/members/1/orders/300/payment-status")
					.contentType("application/json")
					.content("{\"paymentStatus\":\"PAID\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteOrderEndpointIsNotExposed() throws Exception {
		mockMvc.perform(delete("/api/members/1/orders/300"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void legacyOrderApiIsNotExposed() throws Exception {
		mockMvc.perform(get("/api/orders/user/1"))
				.andExpect(status().isNotFound());
	}

	private OrderResponse response() {
		return response(Order.OrderStatus.PENDING);
	}

	private OrderResponse cancelledResponse() {
		return response(Order.OrderStatus.CANCELLED);
	}

	private OrderResponse response(Order.OrderStatus status) {
		return new OrderResponse(
				300L, 1L, OrderType.GENERAL, null, null, status,
				Order.PaymentStatus.UNPAID, null, null, null, List.of()
		);
	}
}
