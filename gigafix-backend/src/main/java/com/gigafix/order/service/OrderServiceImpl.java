package com.gigafix.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.order.dto.CreateOrderRequest;
import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.dto.PaymentSuccessRequest;
import com.gigafix.order.dto.ShipOrderRequest;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.product.constant.ProductSaleStatus;
import com.gigafix.product.entity.Product;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.product.repository.ProductDao;
import com.gigafix.product.service.ProductService;
import com.gigafix.order.constant.OrderStatus;
import com.gigafix.order.constant.PaymentStatus;
import com.gigafix.order.constant.ShippingStatus;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    // 訂單 Repository
    private final OrderRepository orderRepository;

    // 訂單明細 Repository
    private final OrderItemRepository orderItemRepository;

    // 購物車 Repository
    private final CartItemRepository cartItemRepository;

    // 會員 Repository
    private final MemberRepository memberRepository;

    // 商品 Repository
    private final ProductDao productDao;
    // 商品 Service
    private final ProductService productService;

    @Override
    @Transactional
    public OrderResponse createOrder(
            Long memberId,
            CreateOrderRequest request) {

        // 檢查會員是否存在
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "會員不存在，memberId：" + memberId));

        // 查詢會員購物車內全部商品
        List<CartItem> cartItems = cartItemRepository.findByMember_Id(memberId);
        // 檢查購物車是否為空
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("購物車是空的，無法建立訂單");
        }

        // 檢查商品狀態並計算訂單總金額
        Integer totalAmount = 0;

        for (CartItem cartItem : cartItems) {
            Product product = productDao.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "商品不存在，productId：" + cartItem.getProductId()));
            // 商品必須是可販售狀態
            if (product.getSaleStatus() != ProductSaleStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "商品目前不可購買，productId：" + product.getProductId());
            }
            // 累加訂單總金額
            totalAmount += product.getPrice();
        }
        // 建立訂單主表
        Order order = new Order();

        order.setMember(member);
        order.setTotalAmount(totalAmount);
        order.setOrderStatus(OrderStatus.PENDING.name());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.UNPAID.name());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setShippingMethod(request.getShippingMethod());
        order.setShippingStatus(ShippingStatus.PENDING.name());
        order.setCustomerRemark(request.getCustomerRemark());

        // 儲存訂單主表
        Order savedOrder = orderRepository.save(order);

        // 建立訂單明細
        for (CartItem cartItem : cartItems) {

            Product product = productDao.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "商品不存在，productId：" + cartItem.getProductId()));

            OrderItem orderItem = new OrderItem();

            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getProductName());
            orderItem.setUnitPrice(product.getPrice());

            orderItemRepository.save(orderItem);
        }
        // 將結帳商品設為 RESERVED
        for (CartItem cartItem : cartItems) {
            productService.reserveProduct(cartItem.getProductId());
        }
        // 清空購物車
        cartItemRepository.deleteAll(cartItems);

        // 建立回傳 DTO
        return toOrderResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getOrders(Long memberId) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢會員所有訂單
        List<Order> orders = orderRepository.findByMember_Id(memberId);

        // 準備回傳清單
        List<OrderResponse> responseList = new ArrayList<>();

        // 將每一筆 Order 轉成 OrderResponse
        for (Order order : orders) {

            OrderResponse response = toOrderResponse(order);

            responseList.add(response);
        }
        return responseList;
    }

    @Override
    public OrderResponse getOrder(Long memberId, Long orderId) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢指定訂單
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "訂單不存在，orderId：" + orderId));

        // 檢查訂單是否屬於該會員
        if (!order.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException(
                    "訂單不屬於該會員，memberId：" + memberId + ", orderId：" + orderId);
        }

        // 建立回傳 DTO
        return toOrderResponse(order);
    }

    @Transactional
    @Override
    public OrderResponse payOrder(
            Long memberId,
            Long orderId,
            PaymentSuccessRequest request) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢訂單
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "訂單不存在，orderId：" + orderId));

        // 確認訂單屬於此會員
        if (!order.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("無權操作此訂單");
        }
        // 已取消訂單不能付款
        if (OrderStatus.CANCELLED.name().equals(order.getOrderStatus())) {
            throw new IllegalStateException("訂單已取消，無法付款");
        }
        // 防止重複付款
        if (PaymentStatus.PAID.name().equals(order.getPaymentStatus())) {
            throw new IllegalStateException("訂單已完成付款");
        }
        // 更新付款資訊
        order.setPaymentStatus(PaymentStatus.PAID.name());
        order.setTransactionId(request.getTransactionId());
        order.setPaidAt(LocalDateTime.now());

        // 儲存付款後的訂單資料
        Order savedOrder = orderRepository.save(order);

        // 查詢訂單內所有商品
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // 付款成功，將商品改為 SOLD
        for (OrderItem orderItem : orderItems) {
            productService.sellProduct(orderItem.getProductId());
        }

        // 回傳 OrderResponse

        return toOrderResponse(savedOrder);
    }

    @Transactional
    @Override
    public OrderResponse cancelOrder(
            Long memberId,
            Long orderId) {

        // 檢查會員是否存在
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢訂單
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "訂單不存在，orderId：" + orderId));

        // 確認訂單屬於此會員
        if (!order.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("無權操作此訂單");
        }
        // 已付款訂單不能直接取消
        if (PaymentStatus.PAID.name().equals(order.getPaymentStatus())) {
            throw new IllegalStateException(
                    "訂單已付款，請走退款流程，無法直接取消");
        }

        // 只有待處理訂單可以取消
        if (!OrderStatus.PENDING.name().equals(order.getOrderStatus())) {
            throw new IllegalStateException("訂單無法取消");
        }

        // 更新訂單狀態
        order.setOrderStatus(OrderStatus.CANCELLED.name());
        Order savedOrder = orderRepository.save(order);

        // 商品 RESERVED → AVAILABLE
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            productService.releaseProduct(orderItem.getProductId());
        }

        // 回傳 OrderResponse
        return toOrderResponse(savedOrder);
    }

    // 將 Order Entity 轉成 OrderResponse DTO
    private OrderResponse toOrderResponse(Order order) {

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .shippingMethod(order.getShippingMethod())
                .trackingNumber(order.getTrackingNumber())
                .shippingStatus(order.getShippingStatus())
                .customerRemark(order.getCustomerRemark())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional
    @Override
    public OrderResponse shipOrder(
            Long memberId,
            Long orderId,
            ShipOrderRequest request) {

        // 檢查會員
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // 查詢訂單
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "訂單不存在，orderId：" + orderId));

        // 確認訂單屬於會員
        if (!order.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("無權操作此訂單");
        }

        // 確認訂單已付款
        if (!PaymentStatus.PAID.name().equals(order.getPaymentStatus())) {
            throw new IllegalStateException("訂單尚未付款，無法出貨");
        }
        if (OrderStatus.CANCELLED.name().equals(order.getOrderStatus())) {
            throw new IllegalStateException("訂單已取消，無法出貨");
        }
        // 防止重複出貨
        // 只有 PENDING 物流狀態可以出貨
        if (!ShippingStatus.PENDING.name().equals(order.getShippingStatus())) {
            throw new IllegalStateException("訂單目前狀態無法出貨");
        }

        // 更新物流資料
        order.setShippingStatus(ShippingStatus.SHIPPED.name());
        order.setTrackingNumber(request.getTrackingNumber());
        order.setShippedAt(LocalDateTime.now());

        // 回傳 OrderResponse
        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder);
    }

    @Transactional
    @Override
    public OrderResponse deliverOrder(
            Long memberId,
            Long orderId) {

        // ① 檢查會員
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "會員不存在，memberId：" + memberId);
        }

        // ② 查詢訂單
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "訂單不存在，orderId：" + orderId));

        // ③ 確認訂單屬於會員
        if (!order.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("無權操作此訂單");
        }

        // ④ 只有已出貨訂單才能標記送達
        if (!ShippingStatus.SHIPPED.name().equals(order.getShippingStatus())) {
            throw new IllegalStateException(
                    "訂單尚未出貨，無法標記為已送達");
        }

        // ⑤ 更新物流狀態
        order.setShippingStatus(ShippingStatus.DELIVERED.name());
        order.setDeliveredAt(LocalDateTime.now());

        // ⑥ 儲存並回傳
        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }
}