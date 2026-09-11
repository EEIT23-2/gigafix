package com.gigafix.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.cart.entity.CartItem;
import com.gigafix.cart.repository.CartItemRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.order.constant.OrderStatus;
import com.gigafix.order.constant.PaymentStatus;
import com.gigafix.order.constant.ShippingStatus;
import com.gigafix.order.dto.AdminCreateOrderRequest;
import com.gigafix.order.dto.AdminOrderCreateOptionsResponse;
import com.gigafix.order.dto.CreateOrderRequest;
import com.gigafix.order.dto.OrderItemResponse;
import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.dto.PaymentSuccessRequest;
import com.gigafix.order.dto.ShipOrderRequest;
import com.gigafix.order.dto.UpdateOrderRequest;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.product.constant.ProductSaleStatus;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import com.gigafix.product.service.ProductService;
import com.gigafix.coupon.entity.Coupon;
import com.gigafix.coupon.service.CouponService;

import lombok.RequiredArgsConstructor;

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

        // 優惠券 Service
        private final CouponService couponService;

        // 訂單通知 Service
        private final OrderNotificationService orderNotificationService;

        // 管理員預設的付款方式與配送方式
        private static final String ADMIN_PAYMENT_METHOD = "CREDIT_CARD";

        // 管理員預設的配送方式
        private static final String ADMIN_SHIPPING_METHOD = "HOME";
        // ---------------會員前台功能----------------------

        // 會員從購物車結帳建立訂單
        @Transactional
        @Override
        public OrderResponse createOrder(
                        Long memberId,
                        CreateOrderRequest request) {

                // 1. 檢查會員是否存在
                Member member = memberRepository.findById(memberId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "會員不存在，memberId：" + memberId));

                // 2. 取得前端勾選的購物車項目 ID
                List<Long> cartItemIds = request.getCartItemIds();

                if (cartItemIds == null || cartItemIds.isEmpty()) {
                        throw new IllegalStateException("請至少選擇一項商品進行結帳");
                }

                // 3. 只取得這次勾選的購物車商品
                List<CartItem> selectedCartItems = new ArrayList<>();

                for (Long cartItemId : cartItemIds) {

                        CartItem cartItem = cartItemRepository
                                        .findByCartItemIdAndMember_Id(cartItemId, memberId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "購物車商品不存在或不屬於目前會員，cartItemId：" + cartItemId));

                        selectedCartItems.add(cartItem);
                }

                // 4. 檢查商品狀態並計算本次結帳總金額
                Integer totalAmount = 0;

                for (CartItem cartItem : selectedCartItems) {

                        Product product = productDao.findById(cartItem.getProductId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "商品不存在，productId：" + cartItem.getProductId()));

                        if (product.getSaleStatus() != ProductSaleStatus.AVAILABLE) {
                                throw new IllegalStateException(
                                                "商品目前不可購買，productId：" + product.getProductId());
                        }

                        totalAmount += product.getPrice();
                }
                // 取得前端送出的優惠券代碼
                String couponCode = request.getCouponCode();

                if (couponCode != null && !couponCode.isBlank()) {

                        Coupon coupon = couponService.validateCoupon(couponCode);

                        int discountAmount = coupon.getDiscountAmount();

                        totalAmount = Math.max(totalAmount - discountAmount, 0);
                }
                // 驗證配送方式並取得實際保存地址
                String shippingMethod = normalizeShippingMethod(
                                request.getShippingMethod());

                String receiverAddress = resolveReceiverAddress(
                                request,
                                shippingMethod);

                // 5. 建立訂單主表
                Order order = new Order();

                order.setMember(member);
                order.setTotalAmount(totalAmount);
                order.setOrderStatus(OrderStatus.PENDING.name());
                order.setPaymentMethod(request.getPaymentMethod());
                order.setPaymentStatus(PaymentStatus.UNPAID.name());
                order.setReceiverName(request.getReceiverName());
                order.setReceiverPhone(request.getReceiverPhone());
                order.setReceiverAddress(receiverAddress);
                order.setShippingMethod(shippingMethod);
                order.setShippingStatus(ShippingStatus.PENDING.name());
                order.setCustomerRemark(request.getCustomerRemark());

                Order savedOrder = orderRepository.save(order);

                // 6. 只建立被勾選商品的訂單明細
                for (CartItem cartItem : selectedCartItems) {

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

                // 7. 只把這次結帳商品設為 RESERVED
                for (CartItem cartItem : selectedCartItems) {
                        productService.reserveProduct(cartItem.getProductId());
                }

                // 8. 只刪除已結帳的購物車項目
                cartItemRepository.deleteAll(selectedCartItems);

                // 9. 回傳訂單
                return toOrderResponse(savedOrder);
        }

        // 查會員自己的全部訂單
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

        // 查會員自己的指定訂單
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

        // 會員付款成功後
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

                // 保留既有模擬付款的「重複付款直接擋掉」
                if (PaymentStatus.PAID
                                .name()
                                .equals(order.getPaymentStatus())) {

                        throw new IllegalStateException(
                                        "訂單已完成付款");
                }

                return completePayment(
                                orderId,
                                request.getTransactionId());
        }

        @Transactional
        @Override
        public OrderResponse completePayment(
                        Long orderId,
                        String transactionId) {

                // 驗證交易編號
                if (transactionId == null
                                || transactionId.isBlank()) {

                        throw new IllegalArgumentException(
                                        "transactionId 不可空白");
                }

                String normalizedTransactionId = transactionId.trim();

                // 查詢訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "訂單不存在，orderId：" + orderId));

                // 已取消訂單不能付款
                if (OrderStatus.CANCELLED
                                .name()
                                .equals(order.getOrderStatus())) {

                        throw new IllegalStateException(
                                        "訂單已取消，無法付款");
                }

                // 處理重複 Callback
                if (PaymentStatus.PAID
                                .name()
                                .equals(order.getPaymentStatus())) {

                        // 同一筆交易重送
                        if (normalizedTransactionId.equals(
                                        order.getTransactionId())) {

                                return toOrderResponse(order);
                        }

                        // 訂單已付款，但交易編號不同
                        throw new IllegalStateException(
                                        "訂單已完成付款");
                }

                // 防止 transactionId 被其他訂單使用
                if (orderRepository.existsByTransactionId(
                                normalizedTransactionId)) {

                        throw new IllegalStateException(
                                        "交易編號已被使用");
                }

                // 更新付款資訊
                order.setPaymentStatus(
                                PaymentStatus.PAID.name());

                order.setTransactionId(
                                normalizedTransactionId);

                order.setPaidAt(
                                LocalDateTime.now());

                Order savedOrder = orderRepository.save(order);

                // 查詢訂單商品
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(
                                orderId);

                // RESERVED → SOLD
                for (OrderItem orderItem : orderItems) {

                        productService.sellProduct(
                                        orderItem.getProductId());
                }
                // 付款與商品狀態全部更新完成後，寄付款成功 Email
                orderNotificationService
                                .sendPaymentSuccessEmail(savedOrder);

                return toOrderResponse(savedOrder);
        }

        // 會員取消自己的未付款訂單
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

        // ---------------管理員後台功能----------------------

        // 管理員指定會員與商品建立訂單
        @Transactional
        @Override
        public OrderResponse createOrderByAdmin(
                        AdminCreateOrderRequest request) {

                // 查詢會員
                Member member = memberRepository.findById(request.getMemberId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "會員不存在，memberId：" + request.getMemberId()));
                // 檢查是否有指定商品
                if (request.getProductIds() == null
                                || request.getProductIds().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "至少需要指定一個商品");
                }
                // 檢查商品狀態並計算訂單總金額
                Integer totalAmount = 0;

                for (Long productId : request.getProductIds()) {

                        // 查詢商品
                        Product product = productDao.findById(productId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "商品不存在，productId：" + productId));

                        // 商品必須為可販售狀態
                        if (product.getSaleStatus() != ProductSaleStatus.AVAILABLE) {

                                throw new IllegalStateException(
                                                "商品目前不可建立訂單，productId："
                                                                + product.getProductId());
                        }

                        // 累加訂單總金額
                        totalAmount += product.getPrice();
                }
                // 整理收件資料，避免儲存前後空白
                String receiverName = request.getReceiverName().trim();

                String receiverPhone = request.getReceiverPhone().trim();

                String receiverAddress = request.getReceiverAddress().trim();

                String customerRemark = request.getCustomerRemark();

                if (customerRemark != null) {
                        customerRemark = customerRemark.trim();

                        if (customerRemark.isEmpty()) {
                                customerRemark = null;
                        }
                }
                // 建立訂單主表
                Order order = new Order();

                order.setMember(member);
                order.setTotalAmount(totalAmount);
                order.setOrderStatus(OrderStatus.PENDING.name());
                order.setPaymentMethod(request.getPaymentMethod());
                order.setPaymentStatus(PaymentStatus.UNPAID.name());
                order.setReceiverName(receiverName);
                order.setReceiverPhone(receiverPhone);
                order.setReceiverAddress(receiverAddress);
                order.setShippingMethod(request.getShippingMethod());
                order.setShippingStatus(ShippingStatus.PENDING.name());
                order.setCustomerRemark(customerRemark);

                // 儲存訂單主表
                Order savedOrder = orderRepository.save(order);
                // 建立訂單明細
                for (Long productId : request.getProductIds()) {

                        Product product = productDao.findById(productId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "商品不存在，productId：" + productId));

                        OrderItem orderItem = new OrderItem();

                        orderItem.setOrderId(savedOrder.getOrderId());
                        orderItem.setProductId(product.getProductId());
                        orderItem.setProductName(product.getProductName());
                        orderItem.setUnitPrice(product.getPrice());

                        orderItemRepository.save(orderItem);
                }
                // 將訂單商品設為 RESERVED
                for (Long productId : request.getProductIds()) {

                        productService.reserveProduct(productId);
                }
                // 回傳訂單資料
                return toOrderResponse(savedOrder);
        }

        // 查詢所有會員訂單
        @Override
        public List<OrderResponse> getAllOrders() {

                // 查詢所有訂單
                List<Order> orders = orderRepository.findAll();

                // 準備回傳清單
                List<OrderResponse> responseList = new ArrayList<>();

                // 將每一筆 Order 轉成 OrderResponse
                for (Order order : orders) {

                        OrderResponse response = toOrderResponse(order);

                        responseList.add(response);
                }

                return responseList;
        }

        // 依會員 ID 查詢該會員所有訂單
        @Override
        public List<OrderResponse> getOrdersByMemberId(Long memberId) {

                // 檢查會員是否存在
                if (!memberRepository.existsById(memberId)) {
                        throw new IllegalArgumentException(
                                        "會員不存在，memberId：" + memberId);
                }

                // 查詢該會員所有訂單
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

        // 管理員查詢指定訂單
        @Override
        public OrderResponse getAdminOrder(Long orderId) {

                // 查詢指定訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "訂單不存在，orderId：" + orderId));

                // 回傳 OrderResponse
                return toOrderResponse(order);
        }

        // 修改未出貨訂單資訊
        @Transactional
        @Override
        public OrderResponse updateOrderInfo(
                        Long orderId,
                        UpdateOrderRequest request) {

                // 查詢訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "訂單不存在，orderId：" + orderId));

                // 已取消訂單不可修改
                if (OrderStatus.CANCELLED.name()
                                .equals(order.getOrderStatus())) {

                        throw new IllegalStateException(
                                        "已取消訂單不可修改");
                }
                // 已付款訂單不能修改
                if (PaymentStatus.PAID.name()
                                .equals(order.getPaymentStatus())) {

                        throw new IllegalStateException(
                                        "已付款訂單不可修改");
                }
                // 只有尚未出貨的訂單可以修改
                if (!ShippingStatus.PENDING.name()
                                .equals(order.getShippingStatus())) {

                        throw new IllegalStateException(
                                        "只有尚未出貨的訂單可以修改");
                }
                // 修改會員下單時輸入的資料
                order.setReceiverName(request.getReceiverName());
                order.setReceiverPhone(request.getReceiverPhone());
                order.setReceiverAddress(request.getReceiverAddress());
                order.setShippingMethod(request.getShippingMethod());
                order.setCustomerRemark(request.getCustomerRemark());

                // 儲存
                Order savedOrder = orderRepository.save(order);

                // 回傳 DTO
                return toOrderResponse(savedOrder);
        }

        // 訂單出貨
        @Transactional
        @Override
        public OrderResponse shipOrder(
                        Long orderId,
                        ShipOrderRequest request) {
                // 整理並驗證物流追蹤編號
                String trackingNumber = request.getTrackingNumber();

                if (trackingNumber == null || trackingNumber.isBlank()) {
                        throw new IllegalArgumentException(
                                        "物流追蹤編號不可空白");
                }

                trackingNumber = trackingNumber.trim();

                if (trackingNumber.length() < 3
                                || trackingNumber.length() > 100) {

                        throw new IllegalArgumentException(
                                        "物流追蹤編號長度必須為 3 到 100 個字元");
                }
                // 查詢訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "訂單不存在，orderId：" + orderId));

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
                order.setTrackingNumber(trackingNumber);
                order.setShippedAt(LocalDateTime.now());

                // 儲存並回傳
                Order savedOrder = orderRepository.save(order);

                // 出貨狀態更新成功後寄送通知
                orderNotificationService
                                .sendShippingEmail(savedOrder);

                return toOrderResponse(savedOrder);
        }

        // 訂單送達
        @Transactional
        @Override
        public OrderResponse deliverOrder(Long orderId) {

                // 查詢訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "訂單不存在，orderId：" + orderId));

                // 只有已出貨訂單才能標記送達
                if (!ShippingStatus.SHIPPED.name().equals(order.getShippingStatus())) {
                        throw new IllegalStateException(
                                        "訂單尚未出貨，無法標記為已送達");
                }

                // 更新物流狀態
                order.setShippingStatus(ShippingStatus.DELIVERED.name());

                // 訂單完成
                order.setOrderStatus(OrderStatus.COMPLETED.name());

                // 記錄送達時間
                order.setDeliveredAt(LocalDateTime.now());

                // 儲存狀態
                Order savedOrder = orderRepository.save(order);

                // 送達狀態更新成功後寄送通知
                orderNotificationService
                                .sendDeliveredEmail(savedOrder);

                return toOrderResponse(savedOrder);
        }

        // 刪除符合條件的訂單
        @Transactional
        @Override
        public void deleteOrder(Long orderId) {

                // 查詢訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "訂單不存在，orderId：" + orderId));

                // 必須是已取消訂單
                if (!OrderStatus.CANCELLED.name()
                                .equals(order.getOrderStatus())) {

                        throw new IllegalStateException(
                                        "只有已取消訂單可以刪除");
                }

                // 已付款訂單不能刪除
                if (PaymentStatus.PAID.name()
                                .equals(order.getPaymentStatus())) {

                        throw new IllegalStateException(
                                        "已付款訂單不可刪除");
                }

                // 已進入物流流程不能刪除
                if (!ShippingStatus.PENDING.name()
                                .equals(order.getShippingStatus())) {

                        throw new IllegalStateException(
                                        "已進入物流流程的訂單不可刪除");
                }

                // 查詢訂單明細
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

                // 先刪除訂單明細
                orderItemRepository.deleteAll(orderItems);

                // 再刪除訂單主表
                orderRepository.delete(order);
        }

        // 標準化物流方式，確保為 宅配 或 超取
        private String normalizeShippingMethod(
                        String shippingMethod) {

                if (shippingMethod == null
                                || shippingMethod.isBlank()) {

                        throw new IllegalArgumentException(
                                        "物流方式不能為空");
                }

                String value = shippingMethod
                                .trim()
                                .toUpperCase(Locale.ROOT);

                if (!"HOME".equals(value)
                                && !"STORE".equals(value)) {

                        throw new IllegalArgumentException(
                                        "不支援的物流方式：" + shippingMethod);
                }

                return value;
        }

        private String resolveReceiverAddress(
                        CreateOrderRequest request,
                        String shippingMethod) {

                if ("HOME".equals(shippingMethod)) {

                        if (isBlank(request.getReceiverAddress())) {
                                throw new IllegalArgumentException(
                                                "宅配地址不能為空");
                        }

                        return request
                                        .getReceiverAddress()
                                        .trim();
                }

                validateStoreInformation(request);

                String storeBrand = getStoreBrandName(
                                request.getStoreType());

                return String.format(
                                "%s %s｜%s｜門市代號 %s",
                                storeBrand,
                                request.getStoreName().trim(),
                                request.getStoreAddress().trim(),
                                request.getStoreId().trim());
        }

        private void validateStoreInformation(
                        CreateOrderRequest request) {

                if (isBlank(request.getStoreType())) {
                        throw new IllegalArgumentException(
                                        "請選擇超商品牌");
                }

                if (isBlank(request.getStoreId())) {
                        throw new IllegalArgumentException(
                                        "請先選擇取貨門市");
                }

                if (isBlank(request.getStoreName())) {
                        throw new IllegalArgumentException(
                                        "門市名稱不能為空");
                }

                if (isBlank(request.getStoreAddress())) {
                        throw new IllegalArgumentException(
                                        "門市地址不能為空");
                }
        }

        private String getStoreBrandName(
                        String storeType) {

                String value = storeType
                                .trim()
                                .toUpperCase(Locale.ROOT);

                return switch (value) {

                        case "UNIMART" ->
                                "7-ELEVEN";

                        case "FAMI" ->
                                "全家";

                        case "HILIFE" ->
                                "萊爾富";

                        default ->
                                throw new IllegalArgumentException(
                                                "不支援的超商品牌：" + storeType);
                };
        }

        private boolean isBlank(String value) {
                return value == null
                                || value.trim().isEmpty();
        }

        // 將 Order Entity 轉成 OrderResponse DTO
        private OrderResponse toOrderResponse(Order order) {
                List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getOrderId())
                                .stream()
                                .map(item -> OrderItemResponse.builder()
                                                .productId(item.getProductId())
                                                .productName(item.getProductName())
                                                .unitPrice(item.getUnitPrice())
                                                .build())
                                .toList();
                return OrderResponse.builder()
                                .orderId(order.getOrderId())
                                .memberId(order.getMember().getId())
                                .totalAmount(order.getTotalAmount())
                                .orderItems(items)
                                .orderStatus(order.getOrderStatus())
                                .paymentMethod(order.getPaymentMethod())
                                .paymentStatus(order.getPaymentStatus())
                                .transactionId(order.getTransactionId())
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

        @Override
        public AdminOrderCreateOptionsResponse getAdminCreateOptions() {

                // 查詢所有會員，轉成下拉選單需要的資料
                List<AdminOrderCreateOptionsResponse.MemberOption> members = memberRepository.findAll()
                                .stream()
                                .map(member -> AdminOrderCreateOptionsResponse.MemberOption.builder()
                                                .memberId(member.getId())
                                                .memberName(member.getRealName())
                                                .phone(member.getPhone())
                                                .address(member.getAddress())
                                                .build())
                                .toList();

                // 查詢商品，只保留 AVAILABLE
                List<AdminOrderCreateOptionsResponse.ProductOption> products = productDao.findAll()
                                .stream()
                                .filter(product -> product.getSaleStatus() == ProductSaleStatus.AVAILABLE)
                                .map(product -> AdminOrderCreateOptionsResponse.ProductOption.builder()
                                                .productId(product.getProductId())
                                                .productName(product.getProductName())
                                                .price(product.getPrice())
                                                .build())
                                .toList();

                return AdminOrderCreateOptionsResponse.builder()
                                .members(members)
                                .products(products)
                                .build();
        }

        // 管理員取消未付款訂單
        @Transactional
        @Override
        public OrderResponse adminCancelOrder(Long orderId) {

                // 查詢訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "訂單不存在，orderId：" + orderId));

                // 已付款訂單不能直接取消
                if (PaymentStatus.PAID.name().equals(order.getPaymentStatus())) {
                        throw new IllegalStateException(
                                        "訂單已付款，無法直接取消");
                }

                // 只有待處理訂單可以取消
                if (!OrderStatus.PENDING.name().equals(order.getOrderStatus())) {
                        throw new IllegalStateException(
                                        "只有待處理訂單可以取消");
                }

                // 修改訂單狀態
                order.setOrderStatus(OrderStatus.CANCELLED.name());

                Order savedOrder = orderRepository.save(order);

                // 查詢訂單內商品
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

                // 商品 RESERVED → AVAILABLE
                for (OrderItem orderItem : orderItems) {
                        productService.releaseProduct(
                                        orderItem.getProductId());
                }

                // 回傳取消後的訂單
                return toOrderResponse(savedOrder);
        }
}