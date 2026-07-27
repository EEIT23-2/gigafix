package com.gigafix.cart.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.exception.CartExceptionHandler;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.service.CartService;

import jakarta.servlet.ServletException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CartController.class)
@Import(CartExceptionHandler.class)
class CartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CartService cartService;

	@Test
	@DisplayName("POST 成功新增購物車項目")
	void addCartItem_returnsCreated() throws Exception {
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 2);
		CartItemResponse response = response(1L, 1L, 10L, 2);
		when(cartService.addCartItem(request)).thenReturn(response);

		mockMvc.perform(post("/api/cart-items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cartItemId").value(1))
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.productId").value(10))
				.andExpect(jsonPath("$.quantity").value(2));

		verify(cartService, times(1)).addCartItem(request);
	}

	@Test
	@DisplayName("POST 數量驗證失敗時回傳 400")
	void addCartItem_returnsBadRequestWhenQuantityIsInvalid() throws Exception {
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 0);

		mockMvc.perform(post("/api/cart-items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("quantity")
				));

		verifyNoInteractions(cartService);
	}

	@Test
	@DisplayName("GET 成功回傳會員購物車")
	void getCartItems_returnsTwoItems() throws Exception {
		List<CartItemResponse> responses = List.of(
				response(1L, 1L, 10L, 2),
				response(2L, 1L, 20L, 3)
		);
		when(cartService.getCartItems(1L)).thenReturn(responses);

		mockMvc.perform(get("/api/cart-items/user/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		verify(cartService, times(1)).getCartItems(1L);
	}

	@Test
	@DisplayName("PUT 成功修改購物車數量")
	void updateQuantity_returnsUpdatedItem() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		CartItemResponse response = response(1L, 1L, 10L, 5);
		when(cartService.updateQuantity(1L, request)).thenReturn(response);

		mockMvc.perform(put("/api/cart-items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(5));

		verify(cartService, times(1)).updateQuantity(1L, request);
	}

	@Test
	@DisplayName("PUT 數量驗證失敗時回傳 400")
	void updateQuantity_returnsBadRequestWhenQuantityIsInvalid() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(0);

		mockMvc.perform(put("/api/cart-items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(cartService);
	}

	@Test
	@DisplayName("DELETE 成功刪除購物車項目")
	void deleteCartItem_returnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/cart-items/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		verify(cartService, times(1)).deleteCartItem(1L);
	}

	@Test
	@DisplayName("GET 遇到未處理的 IllegalArgumentException 時向外拋出")
	void getCartItems_propagatesUnhandledIllegalArgumentException() {
		when(cartService.getCartItems(0L))
				.thenThrow(new IllegalArgumentException("userId 必須大於 0"));

		ServletException exception = assertThrows(
				ServletException.class,
				() -> mockMvc.perform(get("/api/cart-items/user/0")
								.contentType(MediaType.APPLICATION_JSON))
						.andReturn()
		);

		assertInstanceOf(IllegalArgumentException.class, exception.getCause());
		verify(cartService, times(1)).getCartItems(0L);
	}

	@Test
	@DisplayName("PUT 找不到購物車項目時回傳 404")
	void updateQuantity_returnsNotFoundWhenItemDoesNotExist() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		when(cartService.updateQuantity(1L, request))
				.thenThrow(new CartItemNotFoundException(1L));

		mockMvc.perform(put("/api/cart-items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("cartItemId")
				));

		verify(cartService, times(1)).updateQuantity(1L, request);
	}

	private CartItemResponse response(
			Long cartItemId,
			Long userId,
			Long productId,
			Integer quantity
	) {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
		LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 1, 10, 30);
		return new CartItemResponse(
				cartItemId,
				userId,
				productId,
				quantity,
				createdAt,
				updatedAt
		);
	}
}
