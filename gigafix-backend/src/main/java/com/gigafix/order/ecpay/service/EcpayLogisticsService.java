package com.gigafix.order.ecpay.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EcpayLogisticsService {

    private static final Set<String> ALLOWED_LOGISTICS_SUB_TYPES = Set.of(
            "UNIMART",
            "FAMI",
            "HILIFE");

    @Value("${ecpay.logistics.merchant-id}")
    private String merchantId;

    @Value("${ecpay.logistics.map-url}")
    private String mapUrl;

    @Value("${ecpay.logistics.callback-url}")
    private String callbackUrl;

    @Value("${app.frontend.checkout-url}")
    private String frontendCheckoutUrl;

    public String buildStoreMapHtml(String logisticsSubType) {

        String subType = normalizeLogisticsSubType(logisticsSubType);

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="zh-Hant">
                <head>
                    <meta charset="UTF-8">
                    <title>前往綠界選擇門市</title>
                </head>
                <body>
                    <p>正在前往綠界選擇取貨門市...</p>

                    <form
                        id="ecpayStoreForm"
                        method="post"
                        action="%s"
                    >
                """.formatted(
                HtmlUtils.htmlEscape(mapUrl)));

        appendHiddenInput(
                html,
                "MerchantID",
                merchantId);

        appendHiddenInput(
                html,
                "LogisticsType",
                "CVS");

        appendHiddenInput(
                html,
                "LogisticsSubType",
                subType);

        appendHiddenInput(
                html,
                "IsCollection",
                "N");

        appendHiddenInput(
                html,
                "ServerReplyURL",
                callbackUrl);

        appendHiddenInput(
                html,
                "Device",
                "0");

        html.append("""
                    </form>

                    <script>
                        document
                            .getElementById('ecpayStoreForm')
                            .submit();
                    </script>
                </body>
                </html>
                """);

        return html.toString();
    }

    public String buildCheckoutRedirectUrl(
            String callbackMerchantId,
            String logisticsSubType,
            String storeId,
            String storeName,
            String storeAddress) {

        if (!merchantId.equals(callbackMerchantId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ECPay MerchantID 不正確");
        }

        String subType = normalizeLogisticsSubType(logisticsSubType);

        if (isBlank(storeId)
                || isBlank(storeName)
                || isBlank(storeAddress)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ECPay 回傳的門市資料不完整");
        }

        return UriComponentsBuilder
                .fromUriString(frontendCheckoutUrl)
                .queryParam(
                        "storeType",
                        subType)
                .queryParam(
                        "storeId",
                        storeId)
                .queryParam(
                        "storeName",
                        storeName)
                .queryParam(
                        "storeAddress",
                        storeAddress)
                .build()
                .encode()
                .toUriString();
    }

    private String normalizeLogisticsSubType(
            String logisticsSubType) {

        if (logisticsSubType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "請選擇超商品牌");
        }

        String value = logisticsSubType
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!ALLOWED_LOGISTICS_SUB_TYPES.contains(value)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "不支援的超商品牌");
        }

        return value;
    }

    private void appendHiddenInput(
            StringBuilder html,
            String name,
            String value) {

        html.append("""
                    <input
                        type="hidden"
                        name="%s"
                        value="%s"
                    >
                """.formatted(
                HtmlUtils.htmlEscape(name),
                HtmlUtils.htmlEscape(value)));
    }

    private boolean isBlank(String value) {
        return value == null
                || value.trim().isEmpty();
    }
}