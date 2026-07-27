package com.gigafix.order.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gigafix.order.dto.request.CreateOrderItemRequest;
import com.gigafix.order.dto.request.CreateOrderRequest;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderItemResponse;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.entity.Order;
import com.gigafix.order.exception.OrderExceptionHandler;
import com.gigafix.order.exception.OrderNotFoundException;
import com.gigafix.order.service.OrderService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(OrderController.class)
@Import(OrderExceptionHandler.class)
class OrderControllerTest {

	private static final LocalDateTime FIXED_TIME =
			LocalDateTime.of(2026, 7, 27, 10, 0);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrderService orderService;

	@Test
	@DisplayName("POST 成功建立訂單")
	void createOrder_returnsCreated() throws Exception {
		CreateOrderRequest request = createOrderRequest();
		OrderResponse response = orderResponse(
				1L,
				1L,
				Order.OrderStatus.PENDING,
				Order.PaymentStatus.UNPAID,
				List.of(orderItemResponse(1L, 10L, 1))
		);
		when(orderService.createOrder(request)).thenReturn(response);

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderId").value(1))
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.totalAmount").value(19960))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
				.andExpect(jsonPath("$.receiverName").value("王小明"))
				.andExpect(jsonPath("$.items[0].productId").value(10))
				.andExpect(jsonPath("$.items[0].quantity").value(1))
				.andExpect(jsonPath("$.items[0].subtotal").value(20000));

		verify(orderService, times(1)).createOrder(request);
	}

	@Test
	@DisplayName("POST 明細為空時回傳 400")
	void createOrder_returnsBadRequestWhenItemsAreEmpty() throws Exception {
		CreateOrderRequest request = new CreateOrderRequest(
				1L,
				"王小明",
				"0912345678",
				"台北市中正區測試路1號",
				new BigDecimal("60"),
				new BigDecimal("100"),
				"請小心配送",
				List.of()
		);

		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("items")
				));

		verifyNoInteractions(orderService);
	}

	@Test
	@DisplayName("GET 成功取得訂單")
	void getOrder_returnsOrderSuccessfully() throws Exception {
		OrderResponse response = orderResponse(
				1L,
				1L,
				Order.OrderStatus.PENDING,
				Order.PaymentStatus.UNPAID,
				List.of(orderItemResponse(1L, 10L, 1))
		);
		when(orderService.getOrder(1L)).thenReturn(response);

		mockMvc.perform(get("/api/orders/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(1))
				.andExpect(jsonPath("$.items.length()").value(1));

		verify(orderService, times(1)).getOrder(1L);
	}

	@Test
	@DisplayName("GET 找不到訂單時回傳 404")
	void getOrder_returnsNotFoundWhenOrderDoesNotExist() throws Exception {
		when(orderService.getOrder(999L))
				.thenThrow(new OrderNotFoundException(999L));

		mockMvc.perform(get("/api/orders/999")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("999")
				))
				.andExpect(jsonPath("$.path").value("/api/orders/999"));

		verify(orderService, times(1)).getOrder(999L);
	}

	@Test
	@DisplayName("GET 成功取得會員的兩筆訂單")
	void getOrdersByUser_returnsTwoOrders() throws Exception {
		List<OrderResponse> responses = List.of(
				orderResponse(
						1L,
						1L,
						Order.OrderStatus.PENDING,
						Order.PaymentStatus.UNPAID,
						List.of(orderItemResponse(1L, 10L, 1))
				),
				orderResponse(
						2L,
						1L,
						Order.OrderStatus.PROCESSING,
						Order.PaymentStatus.PAID,
						List.of(orderItemResponse(2L, 20L, 1))
				)
		);
		when(orderService.getOrdersByUser(1L)).thenReturn(responses);

		mockMvc.perform(get("/api/orders/user/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		verify(orderService, times(1)).getOrdersByUser(1L);
	}

	@Test
	@DisplayName("GET 會員編號不合法時回傳 400")
	void getOrdersByUser_returnsBadRequestWhenUserIdIsInvalid()
			throws Exception {
		when(orderService.getOrdersByUser(0L))
				.thenThrow(new IllegalArgumentException("userId 必須大於 0"));

		mockMvc.perform(get("/api/orders/user/0")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("userId")
				))
				.andExpect(jsonPath("$.path").value("/api/orders/user/0"));

		verify(orderService, times(1)).getOrdersByUser(0L);
	}

	@Test
	@DisplayName("PATCH 成功修改訂單狀態")
	void updateOrderStatus_returnsUpdatedOrder() throws Exception {
		UpdateOrderStatusRequest request =
				new UpdateOrderStatusRequest(Order.OrderStatus.PROCESSING);
		OrderResponse response = orderResponse(
				1L,
				1L,
				Order.OrderStatus.PROCESSING,
				Order.PaymentStatus.UNPAID,
				List.of(orderItemResponse(1L, 10L, 1))
		);
		when(orderService.updateOrderStatus(1L, request))
				.thenReturn(response);

		mockMvc.perform(patch("/api/orders/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PROCESSING"));

		verify(orderService, times(1)).updateOrderStatus(1L, request);
	}

	@Test
	@DisplayName("PATCH 訂單狀態為 null 時回傳 400")
	void updateOrderStatus_returnsBadRequestWhenStatusIsNull()
			throws Exception {
		UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(null);

		mockMvc.perform(patch("/api/orders/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("status")
				));

		verifyNoInteractions(orderService);
	}

	@Test
	@DisplayName("PATCH 成功修改付款狀態")
	void updatePaymentStatus_returnsUpdatedOrder() throws Exception {
		UpdatePaymentStatusRequest request =
				new UpdatePaymentStatusRequest(Order.PaymentStatus.PAID);
		OrderResponse response = orderResponse(
				1L,
				1L,
				Order.OrderStatus.PENDING,
				Order.PaymentStatus.PAID,
				List.of(orderItemResponse(1L, 10L, 1))
		);
		when(orderService.updatePaymentStatus(1L, request))
				.thenReturn(response);

		mockMvc.perform(patch("/api/orders/1/payment-status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentStatus").value("PAID"));

		verify(orderService, times(1))
				.updatePaymentStatus(1L, request);
	}

	@Test
	@DisplayName("PATCH 付款狀態為 null 時回傳 400")
	void updatePaymentStatus_returnsBadRequestWhenPaymentStatusIsNull()
			throws Exception {
		UpdatePaymentStatusRequest request =
				new UpdatePaymentStatusRequest(null);

		mockMvc.perform(patch("/api/orders/1/payment-status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("paymentStatus")
				));

		verifyNoInteractions(orderService);
	}

	@Test
	@DisplayName("DELETE 成功刪除訂單")
	void deleteOrder_returnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/orders/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		verify(orderService, times(1)).deleteOrder(1L);
	}

	@Test
	@DisplayName("DELETE 找不到訂單時回傳 404")
	void deleteOrder_returnsNotFoundWhenOrderDoesNotExist() throws Exception {
		doThrow(new OrderNotFoundException(999L))
				.when(orderService)
				.deleteOrder(999L);

		mockMvc.perform(delete("/api/orders/999")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("999")
				))
				.andExpect(jsonPath("$.path").value("/api/orders/999"));

		verify(orderService, times(1)).deleteOrder(999L);
	}

	private CreateOrderRequest createOrderRequest() {
		return new CreateOrderRequest(
				1L,
				"王小明",
				"0912345678",
				"台北市中正區測試路1號",
				new BigDecimal("60"),
				new BigDecimal("100"),
				"請小心配送",
				List.of(new CreateOrderItemRequest(
						10L,
						"iPhone 15",
						new BigDecimal("20000"),
						1
				))
		);
	}

	private OrderResponse orderResponse(
			Long orderId,
			Long userId,
			Order.OrderStatus status,
			Order.PaymentStatus paymentStatus,
			List<OrderItemResponse> items
	) {
		return new OrderResponse(
				orderId,
				userId,
				FIXED_TIME,
				new BigDecimal("19960.00"),
				status,
				paymentStatus,
				"王小明",
				"0912345678",
				"台北市中正區測試路1號",
				new BigDecimal("60.00"),
				new BigDecimal("100.00"),
				"請小心配送",
				FIXED_TIME,
				FIXED_TIME,
				items
		);
	}

	private OrderItemResponse orderItemResponse(
			Long orderItemId,
			Long productId,
			Integer quantity
	) {
		BigDecimal unitPrice = new BigDecimal("20000.00");
		BigDecimal subtotal = unitPrice.multiply(
				BigDecimal.valueOf(quantity)
		);
		return new OrderItemResponse(
				orderItemId,
				productId,
				"iPhone 15",
				unitPrice,
				quantity,
				subtotal,
				FIXED_TIME,
				FIXED_TIME
		);
	}
}
