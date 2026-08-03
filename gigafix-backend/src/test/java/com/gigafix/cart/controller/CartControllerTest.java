package com.gigafix.cart.controller;

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

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.exception.CartExceptionHandler;
import com.gigafix.cart.exception.DuplicateCartItemException;
import com.gigafix.cart.service.CartService;
import com.gigafix.order.service.OrderService;

/** 驗證正式購物車 API 不再接受或回傳 quantity。 */
@WebMvcTest(CartController.class)
@Import(CartExceptionHandler.class)
@ActiveProfiles("test")
class CartControllerTest {
	@Autowired MockMvc mvc;
	@MockitoBean CartService cartService;
	@MockitoBean OrderService orderService;

	@Test
	void addAcceptsProductIdOnlyAndResponseHasNoQuantity() throws Exception {
		when(cartService.addCartItem(1L, new AddCartItemRequest(20L))).thenReturn(new CartItemResponse(30L, 10L, 20L, null, null));
		mvc.perform(post("/api/members/1/cart/items").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":20}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.productId").value(20)).andExpect(jsonPath("$.quantity").doesNotExist());
	}

	@Test
	void duplicateProductReturnsConflict() throws Exception {
		when(cartService.addCartItem(1L, new AddCartItemRequest(20L))).thenThrow(new DuplicateCartItemException(20L));
		mvc.perform(post("/api/members/1/cart/items").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":20}"))
				.andExpect(status().isConflict());
	}

	@Test
	void updateQuantityEndpointIsNotExposed() throws Exception {
		mvc.perform(patch("/api/members/1/cart/items/30").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void getResponseItemsHaveNoQuantity() throws Exception {
		when(cartService.getActiveCart(1L)).thenReturn(new CartResponse(10L, 1L, Cart.CartStatus.ACTIVE, null, null,
				List.of(new CartItemResponse(30L, 10L, 20L, null, null))));
		mvc.perform(get("/api/members/1/cart")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").doesNotExist());
	}

	@Test void deleteItemReturnsNoContent() throws Exception { mvc.perform(delete("/api/members/1/cart/items/30")).andExpect(status().isNoContent()); }
	@Test void clearReturnsNoContent() throws Exception { mvc.perform(delete("/api/members/1/cart/items")).andExpect(status().isNoContent()); }
}
