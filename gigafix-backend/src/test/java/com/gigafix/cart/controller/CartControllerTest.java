package com.gigafix.cart.controller;

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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.exception.CartExceptionHandler;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartMemberNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.exception.CheckoutNotAvailableException;
import com.gigafix.cart.exception.EmptyCartException;
import com.gigafix.cart.service.CartService;
import com.gigafix.order.exception.OrderMemberNotFoundException;
import com.gigafix.order.service.OrderService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CartController.class)
@Import(CartExceptionHandler.class)
@ActiveProfiles("test")
class CartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CartService cartService;

	@MockitoBean
	private OrderService orderService;

	@Test
	void addCartItemUsesMemberPathAndBodyWithoutMemberId() throws Exception {
		AddCartItemRequest request = new AddCartItemRequest(10L, 2);
		when(cartService.addCartItem(1L, request))
				.thenReturn(new CartItemResponse(
						200L,
						100L,
						10L,
						2,
						null,
						null
				));

		mockMvc.perform(post("/api/members/1/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.productId").value(10));

		verify(cartService).addCartItem(1L, request);
	}

	@Test
	void addCartItemRejectsInvalidQuantity() throws Exception {
		mockMvc.perform(post("/api/members/1/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"productId":10,"quantity":0}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getCartUsesMemberResourcePath() throws Exception {
		when(cartService.getActiveCart(1L)).thenReturn(new CartResponse(
				100L,
				1L,
				Cart.CartStatus.ACTIVE,
				null,
				null,
				List.of()
		));

		mockMvc.perform(get("/api/members/1/cart"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(1));
	}

	@Test
	void missingMemberReturnsNotFound() throws Exception {
		when(cartService.getActiveCart(99L))
				.thenThrow(new CartMemberNotFoundException(99L));

		mockMvc.perform(get("/api/members/99/cart"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(
						"找不到會員，memberId：99"
				));
	}

	@Test
	void updateAnotherMembersItemReturnsNotFound() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(3);
		when(cartService.updateQuantity(1L, 200L, request))
				.thenThrow(new CartItemNotFoundException(200L));

		mockMvc.perform(patch("/api/members/1/cart/items/200")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteAnotherMembersItemReturnsNotFound() throws Exception {
		doThrow(new CartItemNotFoundException(200L))
				.when(cartService).deleteCartItem(1L, 200L);

		mockMvc.perform(delete("/api/members/1/cart/items/200"))
				.andExpect(status().isNotFound());
	}

	@Test
	void checkoutReturnsNotImplementedUntilProductIntegrationExists()
			throws Exception {
		when(orderService.checkoutCart(1L))
				.thenThrow(new CheckoutNotAvailableException());

		mockMvc.perform(post("/api/members/1/cart/checkout"))
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.message").value(
						"商品模組尚未完成，暫時無法建立訂單"
				));
	}

	@Test
	void updateOwnedItemReturnsUpdatedQuantity() throws Exception {
		UpdateCartItemRequest request = new UpdateCartItemRequest(4);
		when(cartService.updateQuantity(1L, 200L, request))
				.thenReturn(new CartItemResponse(
						200L,
						100L,
						10L,
						4,
						null,
						null
				));

		mockMvc.perform(patch("/api/members/1/cart/items/200")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(4));
	}

	@Test
	void legacyCartApiIsNotExposed() throws Exception {
		mockMvc.perform(get("/api/carts/users/1/active"))
				.andExpect(status().isNotFound());
	}

	@Test
	void checkoutMissingMemberReturnsNotFound() throws Exception {
		when(orderService.checkoutCart(99L))
				.thenThrow(new OrderMemberNotFoundException(99L));

		mockMvc.perform(post("/api/members/99/cart/checkout"))
				.andExpect(status().isNotFound());
	}

	@Test
	void checkoutEmptyCartReturnsConflict() throws Exception {
		when(orderService.checkoutCart(1L))
				.thenThrow(new EmptyCartException(100L));

		mockMvc.perform(post("/api/members/1/cart/checkout"))
				.andExpect(status().isConflict());
	}

	@Test
	void checkoutWithoutActiveCartReturnsNotFound() throws Exception {
		when(orderService.checkoutCart(1L))
				.thenThrow(new CartNotFoundException(1L));

		mockMvc.perform(post("/api/members/1/cart/checkout"))
				.andExpect(status().isNotFound());
	}

	@Test
	void checkoutLockConflictReturnsConflict() throws Exception {
		when(orderService.checkoutCart(1L))
				.thenThrow(new PessimisticLockingFailureException(
						"cart lock conflict"
				));

		mockMvc.perform(post("/api/members/1/cart/checkout"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"購物車正在結帳，請稍後再試"
				));
	}
}
