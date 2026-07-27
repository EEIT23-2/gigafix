package com.gigafix.cart.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.exception.CartExceptionHandler;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.service.CartService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CartController.class)
@Import(CartExceptionHandler.class)
class CartControllerTest {

	private static final LocalDateTime FIXED_TIME =
			LocalDateTime.of(2026, 7, 1, 10, 0);

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
		CartItemResponse response = itemResponse(1L, 100L, 10L, 2);
		when(cartService.addCartItem(request)).thenReturn(response);

		mockMvc.perform(post("/api/carts/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cartItemId").value(1))
				.andExpect(jsonPath("$.cartId").value(100))
				.andExpect(jsonPath("$.productId").value(10))
				.andExpect(jsonPath("$.quantity").value(2));

		verify(cartService, times(1)).addCartItem(request);
	}

	@Test
	@DisplayName("POST Validation 失敗時回傳 400")
	void addCartItem_returnsBadRequestWhenQuantityIsInvalid() throws Exception {
		AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 0);

		mockMvc.perform(post("/api/carts/items")
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
	@DisplayName("GET 成功取得 ACTIVE Cart")
	void getActiveCart_returnsCartSuccessfully() throws Exception {
		CartResponse response = cartResponse(
				Cart.CartStatus.ACTIVE,
				List.of(
						itemResponse(1L, 100L, 10L, 2),
						itemResponse(2L, 100L, 20L, 3)
				)
		);
		when(cartService.getActiveCart(1L)).thenReturn(response);

		mockMvc.perform(get("/api/carts/users/1/active")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId").value(100))
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.items.length()").value(2));

		verify(cartService, times(1)).getActiveCart(1L);
	}

	@Test
	@DisplayName("GET userId 不合法時回傳 400")
	void getActiveCart_returnsBadRequestWhenUserIdIsInvalid()
			throws Exception {
		when(cartService.getActiveCart(0L))
				.thenThrow(new IllegalArgumentException("userId 必須大於 0"));

		mockMvc.perform(get("/api/carts/users/0/active")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("userId")
				))
				.andExpect(jsonPath("$.path").value(
						"/api/carts/users/0/active"
				));

		verify(cartService, times(1)).getActiveCart(0L);
	}

	@Test
	@DisplayName("GET 找不到 ACTIVE Cart 時回傳 404")
	void getActiveCart_returnsNotFoundWhenCartDoesNotExist()
			throws Exception {
		when(cartService.getActiveCart(1L))
				.thenThrow(new CartNotFoundException(1L));

		mockMvc.perform(get("/api/carts/users/1/active")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("userId：1")
				))
				.andExpect(jsonPath("$.path").value(
						"/api/carts/users/1/active"
				));

		verify(cartService, times(1)).getActiveCart(1L);
	}

	@Test
	@DisplayName("PATCH 成功修改購物車數量")
	void updateQuantity_returnsUpdatedItem() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);
		CartItemResponse response = itemResponse(1L, 100L, 10L, 5);
		when(cartService.updateQuantity(1L, request)).thenReturn(response);

		mockMvc.perform(patch("/api/carts/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId").value(100))
				.andExpect(jsonPath("$.quantity").value(5));

		verify(cartService, times(1)).updateQuantity(1L, request);
	}

	@Test
	@DisplayName("PATCH Validation 失敗時回傳 400")
	void updateQuantity_returnsBadRequestWhenQuantityIsInvalid() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(0);

		mockMvc.perform(patch("/api/carts/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("quantity")
				));

		verifyNoInteractions(cartService);
	}

	@Test
	@DisplayName("舊 PUT 修改購物車數量不再支援")
	void updateQuantity_putIsNotSupported() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(5);

		mockMvc.perform(put("/api/carts/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().is4xxClientError());

		verifyNoInteractions(cartService);
	}

	@Test
	@DisplayName("DELETE 成功刪除購物車項目")
	void deleteCartItem_returnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/carts/items/1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());

		verify(cartService, times(1)).deleteCartItem(1L);
	}

	@Test
	@DisplayName("DELETE 找不到購物車項目時回傳 404")
	void deleteCartItem_returnsNotFoundWhenItemDoesNotExist() throws Exception {
		doThrow(new CartItemNotFoundException(999L))
				.when(cartService)
				.deleteCartItem(999L);

		mockMvc.perform(delete("/api/carts/items/999")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("999")
				))
				.andExpect(jsonPath("$.path").value(
						"/api/carts/items/999"
				));

		verify(cartService, times(1)).deleteCartItem(999L);
	}

	@Test
	@DisplayName("POST checkout 成功")
	void checkoutCart_returnsCheckedOutCart() throws Exception {
		CartResponse response = cartResponse(
				Cart.CartStatus.CHECKED_OUT,
				List.of(itemResponse(1L, 100L, 10L, 2))
		);
		when(cartService.checkoutCart(1L)).thenReturn(response);

		mockMvc.perform(post("/api/carts/users/1/checkout")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId").value(100))
				.andExpect(jsonPath("$.status").value("CHECKED_OUT"))
				.andExpect(jsonPath("$.items.length()").value(1));

		verify(cartService, times(1)).checkoutCart(1L);
	}

	@Test
	@DisplayName("POST checkout 找不到 Cart 時回傳 404")
	void checkoutCart_returnsNotFoundWhenCartDoesNotExist() throws Exception {
		when(cartService.checkoutCart(1L))
				.thenThrow(new CartNotFoundException(1L));

		mockMvc.perform(post("/api/carts/users/1/checkout")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("userId：1")
				))
				.andExpect(jsonPath("$.path").value(
						"/api/carts/users/1/checkout"
				));

		verify(cartService, times(1)).checkoutCart(1L);
	}

	@Test
	@DisplayName("POST checkout 空購物車時回傳 409")
	void checkoutCart_returnsConflictWhenCartIsEmpty() throws Exception {
		when(cartService.checkoutCart(1L))
				.thenThrow(new EmptyCartException(100L));

		mockMvc.perform(post("/api/carts/users/1/checkout")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value(
						org.hamcrest.Matchers.containsString("cartId：100")
				))
				.andExpect(jsonPath("$.path").value(
						"/api/carts/users/1/checkout"
				));

		verify(cartService, times(1)).checkoutCart(1L);
	}

	private CartItemResponse itemResponse(
			Long cartItemId,
			Long cartId,
			Long productId,
			Integer quantity
	) {
		return new CartItemResponse(
				cartItemId,
				cartId,
				productId,
				quantity,
				FIXED_TIME,
				FIXED_TIME
		);
	}

	private CartResponse cartResponse(
			Cart.CartStatus status,
			List<CartItemResponse> items
	) {
		return new CartResponse(
				100L,
				1L,
				status,
				FIXED_TIME,
				FIXED_TIME,
				items
		);
	}
}
