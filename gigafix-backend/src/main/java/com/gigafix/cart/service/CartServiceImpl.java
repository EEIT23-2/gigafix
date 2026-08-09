package com.gigafix.cart.service;

import java.util.ArrayList;
import java.util.List;
import com.gigafix.cart.dto.AddCartItemRequest;
import com.gigafix.cart.dto.CartItemResponse;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.product.constant.ProductSaleStatus;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    // 購物車 Repository
    private final CartItemRepository cartItemRepository;

    // 會員 Repository
    private final MemberRepository memberRepository;

    // 商品 Repository
    private final ProductDao productDao;

    @Override
    public CartItemResponse addItem(Long memberId, AddCartItemRequest request) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("會員不存在，memberId：" + memberId);
        }

        // 檢查商品是否存在
        Product product = productDao.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException( // 商品不存在
                        "商品不存在，productId：" + request.getProductId()));

        // 檢查商品是否可販售
        if (product.getSaleStatus() != ProductSaleStatus.AVAILABLE) {
            throw new IllegalStateException( // 商品不可販售
                    "商品目前不可加入購物車，productId：" + product.getProductId());
        }

        // 檢查商品是否已存在購物車
        if (cartItemRepository
                .findByMemberIdAndProductId(memberId, request.getProductId())
                .isPresent()) { // =Optional裡面有值，表示商品已存在購物車

            throw new IllegalStateException("商品已存在購物車");
        }
        // 建立購物車明細
        CartItem cartItem = new CartItem();
        cartItem.setMemberId(memberId);
        cartItem.setProductId(request.getProductId());

        // 儲存購物車明細
        CartItem savedItem = cartItemRepository.save(cartItem);

        // 建立回傳 DTO
        return CartItemResponse.builder()
                .cartItemId(savedItem.getCartItemId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .imageUrl(product.getImageUrl())
                .price(product.getPrice())
                .saleStatus(product.getSaleStatus().getCode())
                .createdAt(savedItem.getCreatedAt())
                .build();
    }

    @Override
    public List<CartItemResponse> getCartItems(Long memberId) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢會員購物車內全部商品
        List<CartItem> cartItems = cartItemRepository.findByMemberId(memberId);

        // 準備回傳給前端的資料
        List<CartItemResponse> responseList = new ArrayList<>();

        // 將每一筆 CartItem 轉成 Response
        for (CartItem cartItem : cartItems) {

            // 查詢商品資料
            Product product = productDao.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "商品不存在，productId：" + cartItem.getProductId()));

            // 建立回傳 DTO
            CartItemResponse response = CartItemResponse.builder()
                    .cartItemId(cartItem.getCartItemId())
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .imageUrl(product.getImageUrl())
                    .price(product.getPrice())
                    .saleStatus(product.getSaleStatus().getCode())
                    .createdAt(cartItem.getCreatedAt())
                    .build();

            // 加入回傳清單
            responseList.add(response);
        }
        return responseList;
    }

    @Override
    public void deleteItem(Long memberId, Long cartItemId) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢購物車商品
        CartItem cartItem = cartItemRepository
                .findByCartItemIdAndMemberId(
                        cartItemId,
                        memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "購物車商品不存在"));

        // 刪除購物車商品
        cartItemRepository.delete(cartItem);
    }

    @Override
    public void clearCart(Long memberId) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢會員購物車內全部商品
        List<CartItem> cartItems = cartItemRepository.findByMemberId(memberId);

        // 刪除購物車內全部商品
        cartItemRepository.deleteAll(cartItems);
    }
}
