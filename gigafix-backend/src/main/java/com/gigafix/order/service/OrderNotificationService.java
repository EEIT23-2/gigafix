package com.gigafix.order.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.gigafix.order.entity.Order;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final JavaMailSender mailSender;

    /**
     * 付款成功通知
     *
     * 寄信失敗不能影響已完成的付款流程，
     * 因此這裡自行捕捉例外，不往外拋。
     */
    public void sendPaymentSuccessEmail(Order order) {

        try {

            String email = order.getMember().getEmail();

            if (email == null
                    || email.isBlank()) {

                System.err.println(
                        "付款成功 Email 寄送失敗：會員 Email 為空");

                return;
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8");

            helper.setTo(email);

            helper.setSubject(
                    "【Gigafix機不可失】訂單付款成功通知");

            helper.setText(
                    buildPaymentSuccessContent(order),
                    true);

            mailSender.send(message);

            System.out.println(
                    "付款成功 Email 已寄送，orderId = "
                            + order.getOrderId());

        } catch (Exception e) {

            /*
             * Email 是通知功能，
             * 不能因 SMTP 暫時失敗，
             * 讓已完成的付款被 rollback。
             */
            System.err.println(
                    "付款成功 Email 寄送失敗，orderId = "
                            + order.getOrderId()
                            + "，原因："
                            + e.getMessage());
        }
    }

    // 訂單出貨通知
    public void sendShippingEmail(Order order) {

        try {

            String email = order.getMember().getEmail();

            if (email == null || email.isBlank()) {

                System.err.println(
                        "出貨通知 Email 寄送失敗：會員 Email 為空");

                return;
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8");

            helper.setTo(email);

            helper.setSubject(
                    "【Gigafix機不可失】訂單出貨通知");

            helper.setText(
                    buildShippingContent(order),
                    true);

            mailSender.send(message);

            System.out.println(
                    "出貨通知 Email 已寄送，orderId = "
                            + order.getOrderId());

        } catch (Exception e) {

            // Email 失敗不能影響已完成的出貨狀態
            System.err.println(
                    "出貨通知 Email 寄送失敗，orderId = "
                            + order.getOrderId()
                            + "，原因："
                            + e.getMessage());
        }
    }

    // 建立付款成功 Email 內容
    private String buildPaymentSuccessContent(
            Order order) {

        return """
                <div style="
                    max-width:560px;
                    margin:0 auto;
                    font-family:'Microsoft JhengHei',Arial,sans-serif;
                    border:1px solid #eaeaea;
                    border-radius:12px;
                    overflow:hidden;
                ">

                    <div style="
                        background-color:#1e3557;
                        padding:24px 32px;
                    ">
                        <span style="
                            color:#ffffff;
                            font-size:22px;
                            font-weight:700;
                        ">
                            Gigafix 機不可失
                        </span>
                    </div>

                    <div style="
                        padding:32px;
                        background-color:#ffffff;
                    ">

                        <h2 style="
                            color:#1d324b;
                            margin-top:0;
                        ">
                            付款成功
                        </h2>

                        <p>
                            您的訂單已完成付款，
                            以下為付款資訊：
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:24px;
                        ">

                            <tr>
                                <td style="padding:10px 0;">
                                    訂單編號
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                    font-weight:700;
                                ">
                                    #%s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    實付金額
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                    font-weight:700;
                                ">
                                    NT$ %,d
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    付款狀態
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    已付款
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    交易編號
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    付款時間
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                        </table>

                    </div>

                    <div style="
                        background-color:#f5f7fa;
                        padding:16px 32px;
                        text-align:center;
                    ">
                        <span style="
                            font-size:12px;
                            color:#a0a0a0;
                        ">
                            此信件為系統自動發送，請勿直接回覆
                        </span>
                    </div>

                </div>
                """.formatted(
                order.getOrderId(),
                order.getTotalAmount(),
                order.getTransactionId(),
                order.getPaidAt());
    }

    // 建立出貨通知 Email 內容
    private String buildShippingContent(Order order) {

        String shippingMethod = "STORE".equals(order.getShippingMethod())
                ? "超商取貨"
                : "宅配";
        String addressLabel = "STORE".equals(order.getShippingMethod())
                ? "取貨門市"
                : "配送地址";

        return """
                <div style="
                    max-width:560px;
                    margin:0 auto;
                    font-family:'Microsoft JhengHei',Arial,sans-serif;
                    border:1px solid #eaeaea;
                    border-radius:12px;
                    overflow:hidden;
                ">

                    <div style="
                        background-color:#1e3557;
                        padding:24px 32px;
                    ">
                        <span style="
                            color:#ffffff;
                            font-size:22px;
                            font-weight:700;
                        ">
                            Gigafix 機不可失
                        </span>
                    </div>

                    <div style="
                        padding:32px;
                        background-color:#ffffff;
                    ">

                        <h2 style="
                            color:#1d324b;
                            margin-top:0;
                        ">
                            您的訂單已出貨
                        </h2>

                        <p>
                            您的商品已完成出貨，
                            以下為物流資訊：
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:24px;
                        ">

                            <tr>
                                <td style="padding:10px 0;">
                                    訂單編號
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                    font-weight:700;
                                ">
                                    #%s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    收件人
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    配送方式
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    %s
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    物流追蹤編號
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                    font-weight:700;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    出貨時間
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                        </table>

                    </div>

                    <div style="
                        background-color:#f5f7fa;
                        padding:16px 32px;
                        text-align:center;
                    ">
                        <span style="
                            font-size:12px;
                            color:#a0a0a0;
                        ">
                            此信件為系統自動發送，請勿直接回覆
                        </span>
                    </div>

                </div>
                """.formatted(
                order.getOrderId(),
                order.getReceiverName(),
                shippingMethod,
                addressLabel,
                order.getReceiverAddress(),
                order.getTrackingNumber(),
                order.getShippedAt());
    }

    public void sendDeliveredEmail(Order order) {

        try {

            String email = order.getMember().getEmail();

            if (email == null || email.isBlank()) {

                System.err.println(
                        "送達通知 Email 寄送失敗：會員 Email 為空");

                return;
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8");

            helper.setTo(email);

            helper.setSubject(
                    "【Gigafix機不可失】訂單已送達通知");

            helper.setText(
                    buildDeliveredContent(order),
                    true);

            mailSender.send(message);

            System.out.println(
                    "送達通知 Email 已寄送，orderId = "
                            + order.getOrderId());

        } catch (Exception e) {

            // Email 寄送失敗不能影響已完成的送達狀態
            System.err.println(
                    "送達通知 Email 寄送失敗，orderId = "
                            + order.getOrderId()
                            + "，原因："
                            + e.getMessage());
        }
    }

    // 建立送達通知 Email 內容
    private String buildDeliveredContent(Order order) {

        String shippingMethod = "STORE".equals(order.getShippingMethod())
                ? "超商取貨"
                : "宅配";

        String addressLabel = "STORE".equals(order.getShippingMethod())
                ? "取貨門市"
                : "配送地址";

        return """
                <div style="
                    max-width:560px;
                    margin:0 auto;
                    font-family:'Microsoft JhengHei',Arial,sans-serif;
                    border:1px solid #eaeaea;
                    border-radius:12px;
                    overflow:hidden;
                ">

                    <div style="
                        background-color:#1e3557;
                        padding:24px 32px;
                    ">
                        <span style="
                            color:#ffffff;
                            font-size:22px;
                            font-weight:700;
                        ">
                            Gigafix 機不可失
                        </span>
                    </div>

                    <div style="
                        padding:32px;
                        background-color:#ffffff;
                    ">

                        <h2 style="
                            color:#1d324b;
                            margin-top:0;
                        ">
                            訂單已送達
                        </h2>

                        <p>
                            您的訂單已完成配送，
                            感謝您使用 Gigafix。
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:24px;
                        ">

                            <tr>
                                <td style="padding:10px 0;">
                                    訂單編號
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                    font-weight:700;
                                ">
                                    #%s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    收件人
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    配送方式
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    %s
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    物流追蹤編號
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    配送狀態
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                    font-weight:700;
                                ">
                                    已送達
                                </td>
                            </tr>

                            <tr>
                                <td style="padding:10px 0;">
                                    送達時間
                                </td>

                                <td style="
                                    padding:10px 0;
                                    text-align:right;
                                ">
                                    %s
                                </td>
                            </tr>

                        </table>

                    </div>

                    <div style="
                        background-color:#f5f7fa;
                        padding:16px 32px;
                        text-align:center;
                    ">
                        <span style="
                            font-size:12px;
                            color:#a0a0a0;
                        ">
                            此信件為系統自動發送，請勿直接回覆
                        </span>
                    </div>

                </div>
                """.formatted(
                order.getOrderId(),
                order.getReceiverName(),
                shippingMethod,
                addressLabel,
                order.getReceiverAddress(),
                order.getTrackingNumber(),
                order.getDeliveredAt());
    }
}
