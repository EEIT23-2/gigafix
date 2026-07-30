package com.gigafix.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.exception.CartItemNotFoundException;
import com.gigafix.cart.exception.CartMemberNotFoundException;
import com.gigafix.cart.exception.CartNotFoundException;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.cart.repository.CartRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 購物車模組的商業邏輯與交易服務。
 * 協調會員、購物車與購物車項目 Repository，驗證操作權限後組合成 Controller 所需的回應 DTO。
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final MemberRepository memberRepository;

	/**
	 * 將商品加入會員目前啟用中的購物車；相同商品已存在時會累加數量。
	 *
	 * @param memberId 操作購物車的會員識別碼
	 * @param request 要加入的商品識別碼與數量
	 * @return 儲存後的購物車項目資料
	 */
	public CartItemResponse addCartItem(
			Long memberId,
			AddCartItemRequest request
	) {
		// 先確認會員存在，避免建立沒有會員歸屬的購物車。
		Member member = findMember(memberId);
		validateQuantity(request.quantity());

		// 優先使用既有的 ACTIVE 購物車；沒有時才為會員建立一個。
		Cart cart = cartRepository.findByMemberIdAndStatus(
				memberId,
				Cart.CartStatus.ACTIVE
		).orElseGet(() -> cartRepository.save(Cart.builder()
				.member(member)
				.status(Cart.CartStatus.ACTIVE)
				.build()));

		CartItem cartItem = cartItemRepository
				.findByCartCartIdAndProductId(
						cart.getCartId(),
						request.productId()
				)
				.map(existingItem -> {
					// 相同商品再次加入時，保留原項目並累加數量。
					existingItem.setQuantity(
							existingItem.getQuantity() + request.quantity()
					);
					return existingItem;
				})
				.orElseGet(() -> CartItem.builder()
						.cart(cart)
						.productId(request.productId())
						.quantity(request.quantity())
						.build());

		return toItemResponse(cartItemRepository.save(cartItem));
	}

	/**
	 * 查詢會員目前啟用中的購物車與所有項目。
	 *
	 * @param memberId 要查詢的會員識別碼
	 * @return 購物車及項目清單
	 */
	@Transactional(Transactional.TxType.SUPPORTS)
	public CartResponse getActiveCart(Long memberId) {
		// 查詢前先確認會員存在，以區分會員不存在與購物車不存在。
		findMember(memberId);
		Cart cart = findActiveCart(memberId);
		return toCartResponse(
				cart,
				cartItemRepository.findByCartCartId(cart.getCartId())
		);
	}

	/**
	 * 更新會員購物車中指定項目的數量。
	 *
	 * @param memberId 操作購物車的會員識別碼
	 * @param cartItemId 要更新的購物車項目識別碼
	 * @param request 新的商品數量
	 * @return 更新後的購物車項目資料
	 */
	public CartItemResponse updateQuantity(
			Long memberId,
			Long cartItemId,
			UpdateCartItemRequest request
	) {
		findMember(memberId);
		validateQuantity(request.quantity());

		// 同時以會員與項目識別碼查詢，確認項目屬於目前會員。
		CartItem cartItem = findOwnedCartItem(memberId, cartItemId);
		cartItem.setQuantity(request.quantity());
		return toItemResponse(cartItemRepository.save(cartItem));
	}

	/**
	 * 刪除會員購物車中指定的項目。
	 *
	 * @param memberId 操作購物車的會員識別碼
	 * @param cartItemId 要刪除的購物車項目識別碼
	 */
	public void deleteCartItem(Long memberId, Long cartItemId) {
		findMember(memberId);
		// 只允許刪除目前會員所擁有的購物車項目。
		cartItemRepository.delete(findOwnedCartItem(memberId, cartItemId));
	}

	private Member findMember(Long memberId) {
		validateId(memberId, "memberId");
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new CartMemberNotFoundException(memberId));
	}

	private Cart findActiveCart(Long memberId) {
		return cartRepository.findByMemberIdAndStatus(
				memberId,
				Cart.CartStatus.ACTIVE
		).orElseThrow(() -> new CartNotFoundException(memberId));
	}

	private CartItem findOwnedCartItem(Long memberId, Long cartItemId) {
		validateId(cartItemId, "cartItemId");
		return cartItemRepository.findByCartItemIdAndCartMemberId(
				cartItemId,
				memberId
		).orElseThrow(() -> new CartItemNotFoundException(cartItemId));
	}

	private CartItemResponse toItemResponse(CartItem cartItem) {
		return new CartItemResponse(
				cartItem.getCartItemId(),
				cartItem.getCart().getCartId(),
				cartItem.getProductId(),
				cartItem.getQuantity(),
				cartItem.getCreatedAt(),
				cartItem.getUpdatedAt()
		);
	}

	private CartResponse toCartResponse(Cart cart, List<CartItem> items) {
		return new CartResponse(
				cart.getCartId(),
				cart.getMember().getId(),
				cart.getStatus(),
				cart.getCreatedAt(),
				cart.getUpdatedAt(),
				items.stream().map(this::toItemResponse).toList()
		);
	}

	private void validateId(Long id, String fieldName) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(fieldName + " 必須大於 0");
		}
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("購物車數量必須大於 0");
		}
	}
}
