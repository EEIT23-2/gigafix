package com.gigafix.repair.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.gigafix.repair.entity.Repairs;
import com.gigafix.repair.entity.status.DropoffType;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepairNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    // 預約成功通知
    public void sendBookingConfirmed(Repairs r) {

        try {

            String email = r.getMember().getEmail();

            if (email == null || email.isBlank()) {
                System.err.println("預約成功 Email 寄送失敗：會員 Email 為空，repairId = " + r.getId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(email);
            helper.setSubject("【Gigafix機不可失】預約維修成功通知");
            helper.setText(buildBookingConfirmedContent(r), true);

            mailSender.send(message);

            System.out.println("預約成功 Email 已寄送，repairId = " + r.getId());

        } catch (Exception e) {
            // Email 失敗不能影響已完成的預約
            System.err.println("預約成功 Email 寄送失敗，repairId = " + r.getId() + "，原因：" + e.getMessage());
        }
    }

    // 已報價通知
    public void sendQuoteReady(Repairs r) {

        try {

            String email = r.getMember().getEmail();

            if (email == null || email.isBlank()) {
                System.err.println("報價通知 Email 寄送失敗：會員 Email 為空，repairId = " + r.getId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(email);
            helper.setSubject("【Gigafix機不可失】維修報價通知，請確認是否維修");
            helper.setText(buildQuoteReadyContent(r), true);

            mailSender.send(message);

            System.out.println("報價通知 Email 已寄送，repairId = " + r.getId());

        } catch (Exception e) {
            System.err.println("報價通知 Email 寄送失敗，repairId = " + r.getId() + "，原因：" + e.getMessage());
        }
    }

    // 可取件通知（維修完成待取件 / 拒絕報價後取回，共用同一封信）
    public void sendPickupReady(Repairs r) {

        try {

            String email = r.getMember().getEmail();

            if (email == null || email.isBlank()) {
                System.err.println("可取件通知 Email 寄送失敗：會員 Email 為空，repairId = " + r.getId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(email);
            helper.setSubject("【Gigafix機不可失】裝置已可取件，請安排領回");
            helper.setText(buildPickupReadyContent(r), true);

            mailSender.send(message);

            System.out.println("可取件通知 Email 已寄送，repairId = " + r.getId());

        } catch (Exception e) {
            System.err.println("可取件通知 Email 寄送失敗，repairId = " + r.getId() + "，原因：" + e.getMessage());
        }
    }

    private String repairDetailUrl(Repairs r) {
        return frontendBaseUrl + "/member-center/repair/" + r.getId();
    }

    private String dropoffTypeLabel(DropoffType type) {
        return type == DropoffType.SHIPPING ? "寄送門市" : "親臨門市";
    }

    private String buildBookingConfirmedContent(Repairs r) {

        String contactName = HtmlUtils.htmlEscape(r.getContactName());
        String storeName = HtmlUtils.htmlEscape(r.getStore().getName());
        String storeAddress = HtmlUtils.htmlEscape(r.getStore().getAddress());
        String model = HtmlUtils.htmlEscape(r.getRepairBrand() + " " + r.getRepairModel());

        return """
                <div style="
                    max-width:1120px;
                    margin:0 auto;
                    font-family:'Microsoft JhengHei',Arial,sans-serif;
                    border:2px solid #eaeaea;
                    border-radius:24px;
                    overflow:hidden;
                ">

                    <div style="
                        background-color:#1e3557;
                        padding:48px 64px;
                    ">
                        <span style="
                            color:#ffffff;
                            font-size:44px;
                            font-weight:700;
                        ">
                            Gigafix 機不可失
                        </span>
                    </div>

                    <div style="
                        padding:64px;
                        background-color:#ffffff;
                    ">

                        <h2 style="
                            color:#1d324b;
                            margin-top:0;
                            font-size:48px;
                        ">
                            預約成功
                        </h2>

                        <p style="font-size:28px;">
                            親愛的 %s 您好，您的維修預約已成功送出，
                            以下為預約資訊：
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:48px;
                        ">

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修單編號</td>
                                <td style="padding:20px 0; text-align:right; font-weight:700; font-size:28px;">#%d</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">服務門市</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">門市地址</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">預約日期</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">預約時段</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">送修方式</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修機型</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                        </table>

                    </div>

                    <div style="
                        background-color:#f5f7fa;
                        padding:32px 64px;
                        text-align:center;
                    ">
                        <span style="
                            font-size:24px;
                            color:#a0a0a0;
                        ">
                            此信件為系統自動發送，請勿直接回覆
                        </span>
                    </div>

                </div>
                """.formatted(
                contactName,
                r.getId(),
                storeName,
                storeAddress,
                r.getBookingDate(),
                r.getTimeSlot(),
                dropoffTypeLabel(r.getDropoffType()),
                model);
    }

    private String buildQuoteReadyContent(Repairs r) {

        String contactName = HtmlUtils.htmlEscape(r.getContactName());
        String model = HtmlUtils.htmlEscape(r.getRepairBrand() + " " + r.getRepairModel());
        String inspectionResult = HtmlUtils.htmlEscape(r.getInspectionResult());
        String repairItems = HtmlUtils.htmlEscape(r.getRepairItems());
        String detailUrl = repairDetailUrl(r);

        return """
                <div style="
                    max-width:1120px;
                    margin:0 auto;
                    font-family:'Microsoft JhengHei',Arial,sans-serif;
                    border:2px solid #eaeaea;
                    border-radius:24px;
                    overflow:hidden;
                ">

                    <div style="
                        background-color:#1e3557;
                        padding:48px 64px;
                    ">
                        <span style="
                            color:#ffffff;
                            font-size:44px;
                            font-weight:700;
                        ">
                            Gigafix 機不可失
                        </span>
                    </div>

                    <div style="
                        padding:64px;
                        background-color:#ffffff;
                    ">

                        <h2 style="
                            color:#1d324b;
                            margin-top:0;
                            font-size:48px;
                        ">
                            維修報價已完成
                        </h2>

                        <p style="font-size:28px;">
                            親愛的 %s 您好，您的裝置已完成檢測與報價，
                            請登入會員中心確認是否維修：
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:48px;
                        ">

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修單編號</td>
                                <td style="padding:20px 0; text-align:right; font-weight:700; font-size:28px;">#%d</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修機型</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">檢測結果</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修項目</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">預估費用</td>
                                <td style="padding:20px 0; text-align:right; font-weight:700; font-size:28px;">NT$ %,d</td>
                            </tr>

                        </table>

                        <div style="text-align:center; margin-top:64px;">
                            <a href="%s" style="
                                display:inline-block;
                                background-color:#1e3557;
                                color:#ffffff;
                                text-decoration:none;
                                padding:24px 56px;
                                border-radius:16px;
                                font-weight:700;
                                font-size:28px;
                            ">
                                前往會員中心查看報價
                            </a>
                        </div>

                    </div>

                    <div style="
                        background-color:#f5f7fa;
                        padding:32px 64px;
                        text-align:center;
                    ">
                        <span style="
                            font-size:24px;
                            color:#a0a0a0;
                        ">
                            此信件為系統自動發送，請勿直接回覆
                        </span>
                    </div>

                </div>
                """.formatted(
                contactName,
                r.getId(),
                model,
                inspectionResult,
                repairItems,
                r.getEstimatedCost(),
                detailUrl);
    }

    private String buildPickupReadyContent(Repairs r) {

        String contactName = HtmlUtils.htmlEscape(r.getContactName());
        String model = HtmlUtils.htmlEscape(r.getRepairBrand() + " " + r.getRepairModel());
        String storeName = HtmlUtils.htmlEscape(r.getStore().getName());
        String storeAddress = HtmlUtils.htmlEscape(r.getStore().getAddress());
        String detailUrl = repairDetailUrl(r);

        return """
                <div style="
                    max-width:1120px;
                    margin:0 auto;
                    font-family:'Microsoft JhengHei',Arial,sans-serif;
                    border:2px solid #eaeaea;
                    border-radius:24px;
                    overflow:hidden;
                ">

                    <div style="
                        background-color:#1e3557;
                        padding:48px 64px;
                    ">
                        <span style="
                            color:#ffffff;
                            font-size:44px;
                            font-weight:700;
                        ">
                            Gigafix 機不可失
                        </span>
                    </div>

                    <div style="
                        padding:64px;
                        background-color:#ffffff;
                    ">

                        <h2 style="
                            color:#1d324b;
                            margin-top:0;
                            font-size:48px;
                        ">
                            裝置已可取件
                        </h2>

                        <p style="font-size:28px;">
                            親愛的 %s 您好，您的裝置已處理完成，請登入會員中心選擇取件方式
                            （到店取件或寄件到府；選擇寄件時請填寫收件地址）：
                        </p>

                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin-top:48px;
                        ">

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修單編號</td>
                                <td style="padding:20px 0; text-align:right; font-weight:700; font-size:28px;">#%d</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">維修機型</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">服務門市</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">門市地址</td>
                                <td style="padding:20px 0; text-align:right; font-size:28px;">%s</td>
                            </tr>

                            <tr>
                                <td style="padding:20px 0; font-size:28px;">費用</td>
                                <td style="padding:20px 0; text-align:right; font-weight:700; font-size:28px;">NT$ %,d</td>
                            </tr>

                        </table>

                        <div style="text-align:center; margin-top:64px;">
                            <a href="%s" style="
                                display:inline-block;
                                background-color:#1e3557;
                                color:#ffffff;
                                text-decoration:none;
                                padding:24px 56px;
                                border-radius:16px;
                                font-weight:700;
                                font-size:28px;
                            ">
                                前往會員中心安排取件
                            </a>
                        </div>

                    </div>

                    <div style="
                        background-color:#f5f7fa;
                        padding:32px 64px;
                        text-align:center;
                    ">
                        <span style="
                            font-size:24px;
                            color:#a0a0a0;
                        ">
                            此信件為系統自動發送，請勿直接回覆
                        </span>
                    </div>

                </div>
                """.formatted(
                contactName,
                r.getId(),
                model,
                storeName,
                storeAddress,
                r.getFinalCost(),
                detailUrl);
    }
}
