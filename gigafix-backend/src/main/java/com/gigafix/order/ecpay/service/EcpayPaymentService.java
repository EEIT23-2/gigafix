package com.gigafix.order.ecpay.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.gigafix.order.constant.OrderStatus;
import com.gigafix.order.constant.PaymentStatus;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.order.repository.OrderItemRepository;
import com.gigafix.order.repository.OrderRepository;
import com.gigafix.order.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EcpayPaymentService {

        private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
        private static final DateTimeFormatter TRADE_DATE_FORMATTER = DateTimeFormatter
                        .ofPattern("yyyy/MM/dd HH:mm:ss");
        private static final DateTimeFormatter TRADE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
        private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final OrderService orderService;

        @Value("${ecpay.payment.merchant-id}")
        private String merchantId;

        @Value("${ecpay.payment.hash-key}")
        private String hashKey;

        @Value("${ecpay.payment.hash-iv}")
        private String hashIv;

        @Value("${ecpay.payment.url}")
        private String paymentUrl;

        @Value("${ecpay.payment.return-url}")
        private String returnUrl;

        @Value("${ecpay.payment.result-url}")
        private String resultUrl;

        @Value("${app.frontend.base-url}")
        private String frontendBaseUrl;

        /**
         * 建立 ECPay 信用卡付款 HTML
         *
         * Controller 之後直接把這段 HTML 回傳給網頁
         * 網頁會自動 POST 到綠界付款頁
         */
        public String buildPaymentHtml(Long memberId, Long orderId) {
                Order order = getPayableOrder(memberId, orderId);
                Map<String, String> parameters = buildPaymentParameters(order);
                String checkMacValue = calculateCheckMacValue(parameters);

                parameters.put("CheckMacValue", checkMacValue);
                return buildAutoSubmitHtml(parameters);
        }

        /**
         * 查詢並驗證可以付款的訂單
         */
        private Order getPayableOrder(Long memberId, Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("訂單不存在，orderId：" + orderId));

                if (!order.getMember().getId().equals(memberId)) {
                        throw new IllegalStateException("無權操作此訂單");
                }

                if (OrderStatus.CANCELLED.name().equals(order.getOrderStatus())) {
                        throw new IllegalStateException("訂單已取消，無法付款");
                }

                if (PaymentStatus.PAID.name().equals(order.getPaymentStatus())) {
                        throw new IllegalStateException("訂單已完成付款");
                }

                if (order.getTotalAmount() == null || order.getTotalAmount() <= 0) {
                        throw new IllegalStateException("訂單付款金額不正確");
                }

                return order;
        }

        /**
         * 建立送給 ECPay AioCheckOut V5 的參數
         */
        private Map<String, String> buildPaymentParameters(Order order) {
                Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                Long orderId = order.getOrderId();

                parameters.put("MerchantID", merchantId);
                parameters.put("MerchantTradeNo", generateMerchantTradeNo());
                parameters.put(
                                "MerchantTradeDate",
                                LocalDateTime.now(TAIPEI_ZONE).format(TRADE_DATE_FORMATTER));
                parameters.put("PaymentType", "aio");
                parameters.put("TotalAmount", String.valueOf(order.getTotalAmount()));
                parameters.put("TradeDesc", "GigaFix Order Payment");
                parameters.put("ItemName", buildItemName(orderId));
                parameters.put("ReturnURL", returnUrl);
                parameters.put("ChoosePayment", "Credit");
                parameters.put("EncryptType", "1");
                parameters.put("OrderResultURL", resultUrl);
                parameters.put("NeedExtraPaidInfo", "N");

                // 用來在 Callback 找回 GigaFix Order
                parameters.put("CustomField1", String.valueOf(orderId));
                return parameters;
        }

        /**
         * 產生不超過 20 字元且只包含英數字的 MerchantTradeNo
         * GF+ yyMMddHHmmss + 6 位亂數 = 20 字元
         */
        private String generateMerchantTradeNo() {
                String timePart = LocalDateTime.now(TAIPEI_ZONE).format(TRADE_NO_TIME_FORMATTER);
                StringBuilder randomPart = new StringBuilder();

                for (int i = 0; i < 6; i++) {
                        int index = SECURE_RANDOM.nextInt(RANDOM_CHARS.length());
                        randomPart.append(RANDOM_CHARS.charAt(index));
                }

                return "GF" + timePart + randomPart;
        }

        /**
         * 使用 OrderItem 快照產生 ECPay 商品名稱
         */
        private String buildItemName(Long orderId) {
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

                if (orderItems == null || orderItems.isEmpty()) {
                        return "GigaFix Order " + orderId;
                }

                StringJoiner joiner = new StringJoiner("#");
                for (OrderItem item : orderItems) {
                        String productName = item.getProductName();
                        if (productName == null || productName.isBlank()) {
                                continue;
                        }

                        joiner.add(productName.trim());
                }

                String itemName = joiner.toString();
                if (itemName.isBlank()) {
                        itemName = "GigaFix Order " + orderId;
                }

                // ECPay ItemName 最大 400 字元
                // 為避免剛好截斷中文造成問題
                // Demo 留一些安全空間
                if (itemName.length() > 350) {
                        itemName = itemName.substring(0, 350);
                }

                return itemName;
        }

        /**
         * 產生自動 POST 到 ECPay 的 HTML。
         */
        private String buildAutoSubmitHtml(Map<String, String> parameters) {
                StringBuilder html = new StringBuilder();
                html.append("""
                                <!DOCTYPE html>
                                <html lang="zh-Hant">
                                <head>
                                    <meta charset="UTF-8">
                                    <title>前往綠界付款</title>
                                </head>

                                <body>

                                    <p>
                                        正在前往綠界安全付款頁面...
                                    </p>

                                    <form
                                        id="ecpayPaymentForm"
                                        method="post"
                                        action="%s"
                                    >
                                """.formatted(HtmlUtils.htmlEscape(paymentUrl)));

                parameters.forEach((name, value) -> appendHiddenInput(html, name, value));

                html.append("""
                                    </form>

                                    <script>
                                        document
                                            .getElementById(
                                                'ecpayPaymentForm'
                                            )
                                            .submit();
                                    </script>

                                </body>
                                </html>
                                """);

                return html.toString();
        }

        private void appendHiddenInput(StringBuilder html, String name, String value) {
                html.append("""
                                <input
                                type="hidden"
                                name="%s"
                                value="%s">
                                """.formatted(
                                HtmlUtils.htmlEscape(name),
                                HtmlUtils.htmlEscape(value)));
        }

        public void processPaymentReturn(Map<String, String> callback) {
                // 驗證 ECPay 回傳資料來源
                validateCallbackSource(callback);

                // 驗證付款結果
                String rtnCode = requireCallbackValue(callback, "RtnCode");
                if (!"1".equals(rtnCode)) {
                        String rtnMsg = callback.getOrDefault("RtnMsg", "");
                        System.out.println(
                                        "ECPay 付款未成功，RtnCode：" + rtnCode + "，RtnMsg：" + rtnMsg);
                        return;
                }

                /*
                 * SimulatePaid = 1
                 * 代表綠界後台的「模擬付款通知」
                 * 不是消費者真正完成付款
                 *
                 * 我們目前 Demo 使用 Stage 測試信用卡
                 * 所以不接受 SimulatePaid = 1
                 */
                if ("1".equals(callback.get("SimulatePaid"))) {
                        System.out.println(
                                        "收到 ECPay 模擬付款通知，本系統不更新付款狀態");
                        return;
                }

                // GigaFix orderId
                // 我們建立交易時放在 CustomField1
                String customField1 = requireCallbackValue(callback, "CustomField1");
                Long orderId = parseOrderId(customField1);

                // ECPay 交易編號
                String tradeNo = requireCallbackValue(callback, "TradeNo");

                // ECPay 自己的 MerchantTradeNo
                // 雖然目前沒有存 DB
                // 仍確認 Callback 有帶回
                requireCallbackValue(callback, "MerchantTradeNo");

                // 付款金額
                int tradeAmount = parseTradeAmount(requireCallbackValue(callback, "TradeAmt"));

                // 查詢 GigaFix 訂單
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("訂單不存在，orderId：" + orderId));

                // 核對 ECPay 金額與資料庫金額
                if (order.getTotalAmount() == null || order.getTotalAmount().intValue() != tradeAmount) {
                        throw new IllegalStateException("ECPay 付款金額與訂單金額不一致");
                }

                // 真正完成付款
                // PAID、transactionId、paidAt
                // Product SOLD 都由共用邏輯處理
                orderService.completePayment(orderId, tradeNo);
        }

        /**
         * 驗證 ECPay 回傳資料來源
         */
        private void validateCallbackSource(
                        Map<String, String> callback) {

                // CheckMacValue 必須存在
                String receivedCheckMacValue = requireCallbackValue(
                                callback,
                                "CheckMacValue");

                // 使用相同算法重新計算
                String expectedCheckMacValue = calculateCheckMacValue(callback);

                // 比對檢查碼
                if (!secureEquals(
                                expectedCheckMacValue,
                                receivedCheckMacValue)) {

                        throw new IllegalArgumentException(
                                        "ECPay CheckMacValue 驗證失敗");
                }

                // 驗證 MerchantID
                String callbackMerchantId = requireCallbackValue(
                                callback,
                                "MerchantID");

                if (!merchantId.equals(callbackMerchantId)) {
                        throw new IllegalArgumentException(
                                        "ECPay MerchantID 不正確");
                }
        }

        private String requireCallbackValue(Map<String, String> callback, String key) {
                String value = callback.get(key);
                if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("ECPay Callback 缺少參數：" + key);
                }

                return value.trim();
        }

        private Long parseOrderId(String value) {
                try {
                        long orderId = Long.parseLong(value);
                        if (orderId <= 0) {
                                throw new NumberFormatException();
                        }

                        return orderId;
                } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("ECPay CustomField1 的 orderId 不正確");
                }
        }

        private int parseTradeAmount(String value) {
                try {
                        int amount = Integer.parseInt(value);
                        if (amount <= 0) {
                                throw new NumberFormatException();
                        }

                        return amount;
                } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("ECPay TradeAmt 不正確");
                }
        }

        private boolean secureEquals(String expected, String received) {
                byte[] expectedBytes = expected.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
                byte[] receivedBytes = received.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
                return MessageDigest.isEqual(expectedBytes, receivedBytes);
        }

        /**
         * ECPay AioCheckOut CheckMacValue
         */
        public String calculateCheckMacValue(Map<String, String> sourceParameters) {
                Map<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                sourceParameters.forEach((key, value) -> {
                        if (!"CheckMacValue".equalsIgnoreCase(key)) {
                                sorted.put(key, value == null ? "" : value);
                        }
                });

                StringJoiner joiner = new StringJoiner("&");
                sorted.forEach((key, value) -> joiner.add(key + "=" + value));

                String raw = "HashKey=" + hashKey + "&" + joiner + "&HashIV=" + hashIv;
                String encoded = ecpayUrlEncode(raw).toLowerCase(Locale.ROOT);
                return sha256(encoded).toUpperCase(Locale.ROOT);
        }

        /**
         * ECPay 所要求的 URL Encode 規則
         */
        private String ecpayUrlEncode(String value) {
                String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
                return encoded
                                .replace("%2D", "-")
                                .replace("%5F", "_")
                                .replace("%2E", ".")
                                .replace("%21", "!")
                                .replace("%2A", "*")
                                .replace("%28", "(")
                                .replace("%29", ")");
        }

        /**
         * SHA-256
         */
        private String sha256(String value) {
                try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
                        StringBuilder result = new StringBuilder();

                        for (byte b : hash) {
                                result.append(String.format("%02x", b & 0xff));
                        }

                        return result.toString();
                } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException("SHA-256 不可用", e);
                }
        }

        public String processPaymentResult(
                        Map<String, String> callback) {
                // OrderResultURL 是 Client 端付款結果回傳
                // 這裡只驗證 ECPay 回傳來源
                // 不負責更新付款狀態
                // 真正的付款狀態更新以 ReturnURL 為主
                
                // ============================================
                // 正式部署後 取消註解
                // validateCallbackSource(callback);
                // ============================================

                // 部署前 本機demo用，實際上應該驗證 ECPay 回傳來源
                // 部署後註解
                processPaymentReturn(callback);

                // 找回 GigaFix 訂單
                String customField1 = requireCallbackValue(
                                callback, "CustomField1");

                Long orderId = parseOrderId(customField1);

                // 將使用者帶回訂單詳情
                return UriComponentsBuilder
                                .fromUriString(frontendBaseUrl)
                                .path("/member-center/orders/{orderId}")
                                .queryParam("payment", "result")
                                .buildAndExpand(orderId)
                                .encode()
                                .toUriString();
        }
}
