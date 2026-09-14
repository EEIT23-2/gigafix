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

    /** Spring 提供的郵件寄送元件，實際連線資訊由 application.properties 的 mail 設定提供。 */
    private final JavaMailSender mailSender;

    /**
     * 回收單建立成功後，寄送預約成功通知給提出申請的會員。
     *
     * @param application 已建立完成且包含會員、商品及門市資料的回收單
     */
    public void sendApplicationSuccess(RecycleApplication application) {
        // 從回收單取得會員，後續使用會員信箱作為收件地址。
        Member member = application.getMember();

        // 建立 MIME 郵件，讓通知內容可以使用 HTML 排版。
        MimeMessage message = mailSender.createMimeMessage();

        try {
            // false 表示不包含附件；UTF-8 可正確處理繁體中文主旨及內文。
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            // 設定收件人、郵件主旨，以及由回收單資料產生的 HTML 內文。
            helper.setTo(member.getEmail());
            helper.setSubject("【Gigafix】回收預約申請成功");
            helper.setText(buildApplicationSuccessContent(application), true);
        } catch (MessagingException exception) {
            // 將郵件組裝錯誤轉成執行期例外，交由上層交易與全域例外處理器處理。
            throw new IllegalStateException("無法建立回收預約成功通知", exception);
        }

        // 寄信例外不在此吞掉，讓建立回收單的交易可以回滾並由 API 回報失敗。
        mailSender.send(message);
    }

    /** 將一次性驗證碼寄給回收單所屬會員；OTP 本身不會寫入資料庫或 log。 */
    public void sendAgreementOtp(RecycleApplication application, String otp) {
        // 為估價同意驗證建立一封新的 HTML 郵件。
        MimeMessage message = mailSender.createMimeMessage();

        try {
            // OTP 信件不含附件，並使用 UTF-8 避免中文內容亂碼。
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            // 郵件寄給回收單會員，內文包含回收資料、估價及具時效性的 OTP。
            helper.setTo(application.getMember().getEmail());
            helper.setSubject("【Gigafix】回收估價同意驗證碼");
            helper.setText(buildAgreementOtpContent(application, otp), true);
        } catch (MessagingException exception) {
            // 郵件格式建立失敗時保留原始例外，方便後端追蹤錯誤原因。
            throw new IllegalStateException("無法建立回收估價同意通知", exception);
        }

        // 郵件內容組裝成功後，才交給 SMTP 郵件服務寄出。
        mailSender.send(message);
    }

    /** 回收單結案後通知會員，信件只顯示會員的回收金額，不顯示商品庫存售價。 */
    public void sendCompletionNotice(RecycleApplication application) {
        // 為完成回收通知建立一封新的 HTML 郵件。
        MimeMessage message = mailSender.createMimeMessage();

        try {
            // 完成通知不含附件，並指定 UTF-8 中文編碼。
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            // 使用回收單會員信箱作為收件地址，並放入結案內容。
            helper.setTo(application.getMember().getEmail());
            helper.setSubject("【Gigafix】您的裝置已完成回收");
            helper.setText(buildCompletionContent(application), true);
        } catch (MessagingException exception) {
            // 無法完成郵件組裝時通知上層，不讓結案通知靜默失敗。
            throw new IllegalStateException("無法建立回收完成通知", exception);
        }

        // 將完成回收通知寄送給會員。
        mailSender.send(message);
    }

    /**
     * 將回收單資料組成「預約申請成功」HTML 郵件。
     *
     * @param application 要顯示於信件中的回收單
     * @return 可直接交給 MimeMessageHelper 的 HTML 字串
     */
    private String buildApplicationSuccessContent(RecycleApplication application) {
        Member member = application.getMember();

        // 會員輸入會放入 HTML 信件，因此先跳脫，避免內容被當成 HTML 執行。
        String memberName = HtmlUtils.htmlEscape(member.getRealName());
        String productName = HtmlUtils.htmlEscape(application.getProductName());
        String appearance = HtmlUtils.htmlEscape(application.getAppearance());

        // 回收單尚未指定門市時顯示替代文字；有門市時同樣先跳脫門市名稱。
        String storeName = application.getStores() == null
                ? "尚未指定"
                : HtmlUtils.htmlEscape(application.getStores().getName());

        // 使用文字區塊維護 HTML 版型，再依順序填入會員姓名、單號、商品、外觀及門市。
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

    /**
     * 將回收資料與 OTP 組成「估價同意驗證」HTML 郵件。
     *
     * @param application 等待會員同意估價的回收單
     * @param otp          要讓會員輸入的六位數驗證碼
     * @return 包含估價與 OTP 的 HTML 字串
     */
    private String buildAgreementOtpContent(RecycleApplication application, String otp) {
        // 姓名及商品名稱來自資料庫或會員輸入，放入 HTML 前先進行跳脫。
        String memberName = HtmlUtils.htmlEscape(application.getMember().getRealName());
        String productName = HtmlUtils.htmlEscape(application.getProductName());

        // 尚未設定估價時顯示提示；有金額時加上幣別並套用千分位格式。
        String estimatedPrice = application.getEstimatedPrice() == null
                ? "尚未估價"
                : "NT$ " + String.format("%,d", application.getEstimatedPrice());

        // 依序填入會員姓名、回收單號、商品名稱、估價及 OTP，產生完整 HTML 內容。
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

    /**
     * 將已結案回收單組成「完成回收」HTML 郵件。
     *
     * @param application 已完成資料清除及回收結案的回收單
     * @return 顯示回收結果的 HTML 字串
     */
    private String buildCompletionContent(RecycleApplication application) {
        // 所有可能顯示在 HTML 中的文字欄位都先跳脫，避免破壞郵件版型。
        String memberName = HtmlUtils.htmlEscape(application.getMember().getRealName());
        String productName = HtmlUtils.htmlEscape(application.getProductName());

        // 結案金額不存在時顯示替代文字；存在時轉為方便閱讀的台幣格式。
        String recyclePrice = application.getEstimatedPrice() == null
                ? "尚未提供"
                : "NT$ " + String.format("%,d", application.getEstimatedPrice());

        // 依序填入會員姓名、回收單號、商品名稱及最終回收金額。
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
