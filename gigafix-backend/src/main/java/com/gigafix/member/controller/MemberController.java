package com.gigafix.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.member.dto.UpdatePasswordReq;
import com.gigafix.member.dto.DeleteMemberReq;
import com.gigafix.member.dto.ForgotPasswordReq;
import com.gigafix.member.dto.ForgotPasswordResp;
import com.gigafix.member.dto.GetMemberInfoResp;
import com.gigafix.member.dto.LoginReq;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.LoginResult;
import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.dto.SendOtpReq;
import com.gigafix.member.dto.UpdateMemberInfoReq;
import com.gigafix.member.dto.UpdatedMemberInfoResp;
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


	@PostMapping("/register") //註冊,因為不是只資源操作，而是還有包含驗證所以不適用restful原則
	public ResponseEntity<LoginResp> register(@Valid @RequestBody RegisterReq registerReq) throws Exception {
		//寄信
		memberService.register(registerReq);//會先驗證OTP，通過才會真的註冊及登入，所以回傳登入的dto
		LoginResult loginResult = memberService.login(LoginReq.builder().email(registerReq.email()).password(registerReq.password()).build());
		return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, loginResult.responseCookie().toString()).body(loginResult.loginResp()); //201
	}

	@PostMapping("/register/otp") //寄送註冊用的OTP驗證碼，掛在register底下代表這是註冊流程要用的子資源，同樣不套用restful原則
	public ResponseEntity<Void> sendRegisterOtp(@Valid @RequestBody SendOtpReq sendOtpReq) throws Exception {
		mailSenderService.sendRegisterOtp(sendOtpReq.email());
		return ResponseEntity.accepted().build(); //202，代表已受理寄送請求
	}
	
	@PostMapping("/login") //登入，因為不是只資源操作，所以不適用restful原則
	public ResponseEntity<LoginResp> login(@Valid @RequestBody LoginReq loginReq){
		LoginResult loginResult = memberService.login(loginReq);
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, loginResult.responseCookie().toString()).body(loginResult.loginResp()); //200
	}
	
	@PostMapping("/logout") //登出，因為不是只資源操作，所以不適用RESTful原則
	public ResponseEntity<Void> logout(){
		ResponseCookie cookie = memberService.logout();
	    return ResponseEntity.ok()
	            .header(HttpHeaders.SET_COOKIE, cookie.toString())
	            .build();
		//寫完要去把刪除使用者那邊補好，因為刪除使用者會順便把使用者登出
	}
	
	@GetMapping("/me") //取得個人資料的請求
	public ResponseEntity<GetMemberInfoResp> getMemberInfo(@RequestAttribute("memberId") Long memberId){
		GetMemberInfoResp memberInfo = memberService.getMemberInfo(memberId);
		return ResponseEntity.ok(memberInfo);
	}
	
	@PatchMapping("/me") //修改個人資料(可能一個到多個欄位,但前端要把可以修改的欄位資訊傳過來)
	public ResponseEntity<UpdatedMemberInfoResp> updateMemberInfo(@Valid @RequestBody UpdateMemberInfoReq updateMemberInfoReq, @RequestAttribute("memberId") Long memberId){
		//要回傳dto上面的泛型要改下面的return要改
		UpdatedMemberInfoResp updatedMemberInfo = memberService.updateMemberInfo(updateMemberInfoReq, memberId);
		return ResponseEntity.ok(updatedMemberInfo);
	}
	
	// 忘記密碼(登入前)
	@PostMapping("/forgot-password/otp") //寄送忘記密碼用的OTP驗證碼，登入前流程，不套用restful原則
	public ResponseEntity<Void> sendForgotPasswordOtp(@Valid @RequestBody SendOtpReq sendOtpReq) throws Exception {
		mailSenderService.sendForgotPasswordOtp(sendOtpReq.email());
		return ResponseEntity.accepted().build(); //202，代表已受理寄送請求
	}

	@PostMapping("/forgot-password") //忘記密碼(登入前)，mail、新密碼、OTP三者都驗證通過才會真的改密碼
	public ResponseEntity<ForgotPasswordResp> forgotPassword(@Valid @RequestBody ForgotPasswordReq forgotPasswordReq) {
		memberService.forgotPassword(forgotPasswordReq); //內部會先驗證OTP，不符合會拋出InvalidOtpException
		return ResponseEntity.ok(ForgotPasswordResp.builder().email(forgotPasswordReq.email()).build());
	}


	@PatchMapping("/me/password")   //登入後想修改密碼
    public ResponseEntity<String> updatePassword(@Valid @RequestBody UpdatePasswordReq updatePasswordReq,@RequestAttribute("memberId") Long memberId){
		//雖然ChangePasswordReq只有接收前端一個屬性值，但包裝成DTO就可以享有spring 的jackson和validation的支援，而且統一資料的流程控制
		memberService.updatePassword(updatePasswordReq, memberId); //變更密碼不需要傳密碼到前端，也沒有其他更新後的資料要傳送
		return ResponseEntity.noContent().build(); //204
	}
	
	@DeleteMapping("/me") //刪除使用者
	public ResponseEntity<Object> deleteMember(@Valid @RequestBody DeleteMemberReq deleteMemberReq,@RequestAttribute("memberId") Long memberId){
		memberService.deleteMember(deleteMemberReq, memberId); //變更密碼不需要傳密碼到前端，也沒有其他更新後的資料要傳送
		logout();
		return ResponseEntity.noContent().build(); //204
	}
	
	
	
}
