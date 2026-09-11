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

        mailSender.send(message);
    }

    private String buildApplicationSuccessContent(RecycleApplication application) {
        Member member = application.getMember();
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
}
