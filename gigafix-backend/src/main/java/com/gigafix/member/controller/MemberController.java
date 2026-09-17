package com.gigafix.member.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gigafix.member.dto.UpdatePasswordReq;
import com.gigafix.member.exception.InvalidImageFormatException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gigafix.common.util.SecurityUtils;
import com.gigafix.member.dto.DeleteMemberReq;
import com.gigafix.member.dto.ForgotPasswordReq;
import com.gigafix.member.dto.ForgotPasswordResp;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.MemberInfoResp;
import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.dto.SendOtpReq;
import com.gigafix.member.dto.UpdateMemberInfoReq;
import com.gigafix.member.security.MemberUserDetails;
import com.gigafix.member.service.CaptchaService;
import com.gigafix.member.service.MailSenderService;
import com.gigafix.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController("gigaFixUsersController")
@RequestMapping("/api/gigafix/members")
@RequiredArgsConstructor
public class MemberController {
	private final MemberService memberService;
	private final MailSenderService mailSenderService;
	private final CaptchaService captchaService;
	private final Cloudinary cloudinary;

	@PostMapping("/register") // 註冊,因為不是只資源操作，而是還有包含驗證所以不適用restful原則
	public ResponseEntity<LoginResp> register(@Valid @RequestBody RegisterReq registerReq) throws Exception {
		// 註冊成功後直接發JWT讓使用者自動登入，不用另外呼叫登入方法重新驗證一次
		RegisterAndLoginResult registerResult = memberService.register(registerReq);// 會先驗證OTP，通過才會真的註冊
		return ResponseEntity.status(HttpStatus.CREATED)
				.header(HttpHeaders.SET_COOKIE, registerResult.responseCookie().toString())
				.body(registerResult.loginResp()); // 201
	}

	@PostMapping("/register/otp") // 寄送註冊用的OTP驗證碼，掛在register底下代表這是註冊流程要用的子資源，同樣不套用restful原則
	public ResponseEntity<Void> sendRegisterOtp(@Valid @RequestBody SendOtpReq sendOtpReq) throws Exception {
		captchaService.verify(sendOtpReq.captchaToken()); // 先過人機驗證才寄信，擋機器人狂發OTP信件/大量註冊
		mailSenderService.sendRegisterOtp(sendOtpReq.email());
		return ResponseEntity.accepted().build(); // 202，代表已受理寄送請求
	}

	@PostMapping("/logout") // 登出，因為不是只資源操作，所以不適用RESTful原則
	public ResponseEntity<Void> logout() {
		ResponseCookie cookie = memberService.logout();
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.build();
		// 寫完要去把刪除使用者那邊補好，因為刪除使用者會順便把使用者登出
	}

	@GetMapping("/me") // 取得個人資料的請求
	public ResponseEntity<MemberInfoResp> getMemberInfo(Authentication authentication) {
		MemberUserDetails memberDetails = SecurityUtils.getCurrentMember(authentication);
		MemberInfoResp memberInfo = memberService.getMemberInfo(memberDetails.getId());
		return ResponseEntity.ok(memberInfo);
	}

	@PatchMapping("/me") // 修改個人資料(可能一個到多個欄位,但前端要把可以修改的欄位資訊傳過來)
	public ResponseEntity<MemberInfoResp> updateMemberInfo(
			@Valid @RequestBody UpdateMemberInfoReq updateMemberInfoReq, Authentication authentication) {
		MemberUserDetails memberDetails = SecurityUtils.getCurrentMember(authentication);
		MemberInfoResp updatedMemberInfo = memberService.updateMemberInfo(updateMemberInfoReq,
				memberDetails.getId());
		return ResponseEntity.ok(updatedMemberInfo);
	}

	// 忘記密碼(登入前)
	@PostMapping("/forgot-password/otp") // 寄送忘記密碼用的OTP驗證碼，登入前流程，不套用restful原則
	public ResponseEntity<Void> sendForgotPasswordOtp(@Valid @RequestBody SendOtpReq sendOtpReq) throws Exception {
		mailSenderService.sendForgotPasswordOtp(sendOtpReq.email());
		return ResponseEntity.accepted().build(); // 202，代表已受理寄送請求
	}

	@PostMapping("/forgot-password") // 忘記密碼(登入前)，mail、新密碼、OTP三者都驗證通過才會真的改密碼
	public ResponseEntity<ForgotPasswordResp> forgotPassword(@Valid @RequestBody ForgotPasswordReq forgotPasswordReq) {
		memberService.forgotPassword(forgotPasswordReq); // 內部會先驗證OTP，不符合會拋出InvalidOtpException
		return ResponseEntity.ok(ForgotPasswordResp.builder().email(forgotPasswordReq.email()).build());
	}

	@PatchMapping("/me/password") // 登入後想修改密碼
	public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordReq updatePasswordReq,
			Authentication authentication) {
		MemberUserDetails memberDetails = SecurityUtils.getCurrentMember(authentication);
		// 雖然ChangePasswordReq只有接收前端一個屬性值，但包裝成DTO就可以享有spring
		// 的jackson和validation的支援，而且統一資料的流程控制
		memberService.updatePassword(updatePasswordReq, memberDetails.getId()); // 變更密碼不需要傳密碼到前端，也沒有其他更新後的資料要傳送
		return ResponseEntity.noContent().build(); // 204
	}

	@PatchMapping("/me/profileImage")
	public ResponseEntity<MemberInfoResp> updateProfileImage(@RequestParam("file") MultipartFile file,
			Authentication authentication) throws IOException {
		byte[] bytes = file.getBytes();
		// 進來的檔案要做格式驗證為圖檔(不檢查副檔名，而是檢查magic munber)
		if (!isValidImage(bytes)) {// 不是PNG或JPG就拋錯
			throw new InvalidImageFormatException();// 415
		}
		MemberUserDetails memberDetails = SecurityUtils.getCurrentMember(authentication);
		// 參數一檔案，參數二上傳選項(可以設定上傳的資料夾、檔名等等
		Map result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap("folder", "gigafix/avatars"));
		String profileImageUrl = (String) result.get("secure_url");
		MemberInfoResp memberInfo = memberService.updateAvatar(profileImageUrl, memberDetails.getId());
		return ResponseEntity.ok(memberInfo); // 200
	}

	@DeleteMapping("/me") // 刪除使用者
	public ResponseEntity<Void> deleteMember(@Valid @RequestBody DeleteMemberReq deleteMemberReq,
			Authentication authentication) {
		MemberUserDetails memberDetails = SecurityUtils.getCurrentMember(authentication);
		memberService.deleteMember(deleteMemberReq, memberDetails.getId()); // 變更密碼不需要傳密碼到前端，也沒有其他更新後的資料要傳送
		logout();
		return ResponseEntity.noContent().build(); // 204
	}

	@PostMapping("/registerOrLoginAFakeMember")
	public ResponseEntity<LoginResp> registerOrLoginAFakeMember() {
		RegisterAndLoginResult registerResult = memberService.FakeMemberRegisterAndLogin();

		return ResponseEntity.status(HttpStatus.CREATED)
				.header(HttpHeaders.SET_COOKIE, registerResult.responseCookie().toString())
				.body(registerResult.loginResp()); // 201
	}

	private static boolean isValidImage(byte[] bytes) {
		if (bytes.length < 12) {
			return false;
		}
		// jpg的檔案特徵碼(magic number)→FF D8 FF
		if ((bytes[0] & 0xFF) == 0xFF && (bytes[2] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
			// 因為java的byte是有號整數，圖片資料的byte如果超過64第八位會是1的話，在java中會是負數
			/*
			 * 所以必須用位元運算子&把把位數之前的1都去掉(0xFF是16進位FF=255也就是1111
			 * 1111，運算原本的byte就算第八位之後有1也會被去掉)和轉型(變成int的32位元)
			 */
			return true;
		}
		// pen的檔案特徵碼→89 50 4E 47 0D 0A 1A 0A
		if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D
				&& bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
			return true;
		}

		return false;
	}
}
