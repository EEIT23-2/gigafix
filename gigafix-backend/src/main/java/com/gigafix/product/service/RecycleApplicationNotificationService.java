package com.gigafix.product.service;

import com.gigafix.member.entity.Member;
import com.gigafix.product.entity.RecycleApplication;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** 負責寄送 product 模組的回收申請通知，不與訂單模組的郵件流程共用。 */
@Service
@RequiredArgsConstructor
public class RecycleApplicationNotificationService {

    private final JavaMailSender mailSender;

    public void sendApplicationSuccess(RecycleApplication application) {
        Member member = application.getMember();
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(member.getEmail());
            helper.setSubject("【Gigafix】回收預約申請成功");
            helper.setText(buildApplicationSuccessContent(application), true);
        } catch (MessagingException exception) {
            throw new IllegalStateException("無法建立回收預約成功通知", exception);
        }

        // 寄信例外不在此吞掉，讓建立回收單的交易可以回滾並由 API 回報失敗。
        mailSender.send(message);
    }

    /** 將一次性驗證碼寄給回收單所屬會員；OTP 本身不會寫入資料庫或 log。 */
    public void sendAgreementOtp(RecycleApplication application, String otp) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(application.getMember().getEmail());
            helper.setSubject("【Gigafix】回收估價同意驗證碼");
            helper.setText(buildAgreementOtpContent(application, otp), true);
        } catch (MessagingException exception) {
            throw new IllegalStateException("無法建立回收估價同意通知", exception);
        }

        mailSender.send(message);
    }

    /** 回收單結案後通知會員，信件只顯示會員的回收金額，不顯示商品庫存售價。 */
    public void sendCompletionNotice(RecycleApplication application) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(application.getMember().getEmail());
            helper.setSubject("【Gigafix】您的裝置已完成回收");
            helper.setText(buildCompletionContent(application), true);
        } catch (MessagingException exception) {
            throw new IllegalStateException("無法建立回收完成通知", exception);
        }

        mailSender.send(message);
    }

    private String buildApplicationSuccessContent(RecycleApplication application) {
        Member member = application.getMember();
        // 會員輸入會放入 HTML 信件，因此先跳脫，避免內容被當成 HTML 執行。
        String memberName = HtmlUtils.htmlEscape(member.getRealName());
        String productName = HtmlUtils.htmlEscape(application.getProductName());
        String appearance = HtmlUtils.htmlEscape(application.getAppearance());
        String storeName = application.getStores() == null
                ? "尚未指定"
                : HtmlUtils.htmlEscape(application.getStores().getName());

        return """
                <div style="max-width:560px;margin:0 auto;font-family:'Microsoft JhengHei',Arial,sans-serif;border:1px solid #eaeaea;border-radius:12px;overflow:hidden;">
                    <div style="background-color:#1e3557;padding:24px 32px;color:#ffffff;font-size:22px;font-weight:700;">
                        Gigafix 回收服務
                    </div>
                    <div style="padding:32px;background-color:#ffffff;color:#333333;">
                        <h2 style="color:#1d324b;margin-top:0;">回收預約申請成功</h2>
                        <p>%s 您好，我們已收到您的回收預約申請。</p>
                        <table style="width:100%%;border-collapse:collapse;margin-top:24px;">
                            <tr><td style="padding:8px 0;">申請單號</td><td style="padding:8px 0;text-align:right;font-weight:700;">#%s</td></tr>
                            <tr><td style="padding:8px 0;">產品型號</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                            <tr><td style="padding:8px 0;">外觀狀況</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                            <tr><td style="padding:8px 0;">預約門市</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                            <tr><td style="padding:8px 0;">申請狀態</td><td style="padding:8px 0;text-align:right;">已預約交件</td></tr>
                        </table>
                    </div>
                    <div style="background-color:#f5f7fa;padding:16px 32px;text-align:center;color:#888888;font-size:12px;">
                        此信件由系統自動寄送，請勿直接回覆。
                    </div>
                </div>
                """.formatted(
                memberName,
                application.getApplyId(),
                productName,
                appearance,
                storeName);
    }

    private String buildAgreementOtpContent(RecycleApplication application, String otp) {
        String memberName = HtmlUtils.htmlEscape(application.getMember().getRealName());
        String productName = HtmlUtils.htmlEscape(application.getProductName());
        String estimatedPrice = application.getEstimatedPrice() == null
                ? "尚未估價"
                : "NT$ " + String.format("%,d", application.getEstimatedPrice());

        return """
                <div style="max-width:560px;margin:0 auto;font-family:'Microsoft JhengHei',Arial,sans-serif;border:1px solid #eaeaea;border-radius:12px;overflow:hidden;">
                    <div style="background-color:#1e3557;padding:24px 32px;color:#ffffff;font-size:22px;font-weight:700;">
                        Gigafix 回收服務
                    </div>
                    <div style="padding:32px;background-color:#ffffff;color:#333333;">
                        <h2 style="color:#1d324b;margin-top:0;">回收估價同意驗證</h2>
                        <p>%s 您好，回收單 #%s（%s）的參考估價為 <strong>%s</strong>。</p>
                        <p>請在 5 分鐘內輸入以下驗證碼，並完成電子簽名：</p>
                        <div style="margin:24px 0;padding:16px;text-align:center;border:1px dashed #2b77c5;border-radius:10px;background:#eef4fb;color:#2b77c5;font-size:32px;font-weight:800;letter-spacing:8px;">
                            %s
                        </div>
                        <p style="color:#888888;font-size:13px;">若您沒有進行此回收估價確認，請忽略本信件。</p>
                    </div>
                </div>
                """.formatted(
                memberName,
                application.getApplyId(),
                productName,
                estimatedPrice,
                otp);
    }

    private String buildCompletionContent(RecycleApplication application) {
        String memberName = HtmlUtils.htmlEscape(application.getMember().getRealName());
        String productName = HtmlUtils.htmlEscape(application.getProductName());
        String recyclePrice = application.getEstimatedPrice() == null
                ? "尚未提供"
                : "NT$ " + String.format("%,d", application.getEstimatedPrice());

        return """
                <div style="max-width:560px;margin:0 auto;font-family:'Microsoft JhengHei',Arial,sans-serif;border:1px solid #eaeaea;border-radius:12px;overflow:hidden;">
                    <div style="background-color:#1e3557;padding:24px 32px;color:#ffffff;font-size:22px;font-weight:700;">
                        Gigafix 回收服務
                    </div>
                    <div style="padding:32px;background-color:#ffffff;color:#333333;">
                        <h2 style="color:#1d324b;margin-top:0;">回收作業已完成</h2>
                        <p>%s 您好，您的裝置已完成資料清除與回收結案。</p>
                        <table style="width:100%%;border-collapse:collapse;margin-top:24px;">
                            <tr><td style="padding:8px 0;">回收單號</td><td style="padding:8px 0;text-align:right;font-weight:700;">#%s</td></tr>
                            <tr><td style="padding:8px 0;">產品型號</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                            <tr><td style="padding:8px 0;">回收金額</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                            <tr><td style="padding:8px 0;">目前狀態</td><td style="padding:8px 0;text-align:right;color:#24704a;font-weight:700;">完成回收</td></tr>
                        </table>
                        <p style="margin-top:24px;">感謝您使用 Gigafix 回收服務。</p>
                    </div>
                </div>
                """.formatted(
                memberName,
                application.getApplyId(),
                productName,
                recyclePrice);
    }
}
