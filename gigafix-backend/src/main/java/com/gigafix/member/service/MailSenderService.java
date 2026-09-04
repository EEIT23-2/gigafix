package com.gigafix.member.service;

import java.security.SecureRandom;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.gigafix.common.config.CacheConfig;
import com.gigafix.member.exception.InvalidOtpException;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailSenderService {

	private final JavaMailSender mailSender;
	private final CacheManager cacheManager;

	private static final SecureRandom RANDOM = new SecureRandom();

	//寄送註冊用的OTP驗證碼，跟忘記密碼的OTP分開存放在各自的快取，避免兩種用途的驗證碼互相冒用
	public void sendRegisterOtp(String email) throws Exception {
		sendOtp(email, CacheConfig.REGISTER_OTP_CACHE, "【Gigafix機不可失】會員註冊驗證碼", "感謝您註冊 Gigafix 會員");
	}

	//驗證使用者註冊時填入的OTP是否跟快取內的相符，錯誤或逾期(快取內找不到)都拋出InvalidOtpException
	public void verifyRegisterOtp(String email, String otp) {
		verifyOtp(email, otp, CacheConfig.REGISTER_OTP_CACHE);
	}

	//寄送忘記密碼用的OTP驗證碼
	public void sendForgotPasswordOtp(String email) throws Exception {
		sendOtp(email, CacheConfig.FORGOT_PASSWORD_OTP_CACHE, "【Gigafix機不可失】忘記密碼驗證碼", "您正在申請重設 Gigafix 會員密碼");
	}

	//驗證使用者重設密碼時填入的OTP是否跟快取內的相符，錯誤或逾期(快取內找不到)都拋出InvalidOtpException
	public void verifyForgotPasswordOtp(String email, String otp) {
		verifyOtp(email, otp, CacheConfig.FORGOT_PASSWORD_OTP_CACHE);
	}

	//產生6碼亂數後先放進快取(key:email, value:otp)，再用寄信寄給使用者，OTP本身不落地資料庫
	private void sendOtp(String email, String cacheName, String subject, String greeting) throws Exception {
		String otp = generateOtp();
		cacheManager.getCache(cacheName).put(email, otp); //同一信箱重複寄送會直接覆蓋舊碼，且5分鐘沒用到會被Caffeine自動清掉

		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
		helper.setTo(email);
		helper.setSubject(subject);
		helper.setText(buildOtpMailContent(otp, greeting), true); //true代表信件內容是html
		mailSender.send(mimeMessage);
	}

	//驗證通過後立刻把這組OTP從對應的快取移除，避免同一組驗證碼被拿去重複使用
	private void verifyOtp(String email, String otp, String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		Cache.ValueWrapper cached = cache.get(email);
		String cachedOtp = cached == null ? null : (String) cached.get();
		if (cachedOtp == null || !cachedOtp.equals(otp)) {
			throw new InvalidOtpException();
		}
		cache.evict(email);
	}

	//產生6碼數字驗證碼(可能有前導0，所以用字串格式化補滿6碼)
	private String generateOtp() {
		int code = RANDOM.nextInt(1_000_000);
		return String.format("%06d", code);
	}

	//OTP信件內容，套用跟前端主視覺一致的深藍/淺藍配色，排一個簡單好看的版面，greeting用來區分是註冊還是忘記密碼的情境說明
	private String buildOtpMailContent(String otp, String greeting) {
		return """
				<div style="max-width:480px;margin:0 auto;font-family:'Microsoft JhengHei',Arial,sans-serif;border:1px solid #eaeaea;border-radius:12px;overflow:hidden;">
					<div style="background-color:#1e3557;padding:24px 32px;">
						<span style="color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">Gigafix 機不可失</span>
					</div>
					<div style="padding:32px;background-color:#ffffff;">
						<p style="font-size:16px;color:#1d324b;margin:0 0 16px;">您好，%s</p>
						<p style="font-size:15px;color:#555555;margin:0 0 24px;">您的驗證碼為：</p>
						<div style="text-align:center;margin-bottom:24px;">
							<span style="display:inline-block;padding:14px 32px;background-color:#eef4fb;border:1px dashed #2b77c5;border-radius:10px;font-size:32px;font-weight:800;letter-spacing:8px;color:#2b77c5;">%s</span>
						</div>
						<p style="font-size:14px;color:#888888;margin:0 0 6px;">請於5分鐘內完成輸入，逾時請重新取得驗證碼。</p>
						<p style="font-size:14px;color:#888888;margin:0;">若您並未申請此驗證碼，請忽略此封信件。</p>
					</div>
					<div style="background-color:#f5f7fa;padding:16px 32px;text-align:center;">
						<span style="font-size:12px;color:#a0a0a0;">此信件為系統自動發送，請勿直接回覆</span>
					</div>
				</div>
				""".formatted(greeting, otp);
	}

}