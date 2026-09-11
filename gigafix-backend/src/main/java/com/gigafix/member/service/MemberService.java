package com.gigafix.member.service;

import java.time.LocalDateTime;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.gigafix.common.util.JwtUtils;
import com.gigafix.member.dto.UpdatePasswordReq;
import com.gigafix.member.dto.ForgotPasswordReq;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.MemberInfoResp;
import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.dto.UpdateAvatarReq;
import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.dto.UpdateMemberInfoReq;
import com.gigafix.member.dto.DeleteMemberReq;
import com.gigafix.member.entity.Member;
import com.gigafix.member.entity.Member.Gender;
import com.gigafix.member.exception.DuplicateEmailException;
import com.gigafix.member.exception.InvalidCredentialsException;
import com.gigafix.member.exception.MemberNotFoundException;
import com.gigafix.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepository;
	private final JwtUtils jwtUtils;
	private final ObjectMapper objectMapper;
	private final MailSenderService mailSenderService;
	private final PasswordEncoder passwordEncoder;

	// 註冊
	public RegisterAndLoginResult register(@Valid RegisterReq registerReq) throws Exception {
		if (memberRepository.existsByEmail(registerReq.email())) {// 如果有查資料庫mail已被註冊就會回傳true
			throw new DuplicateEmailException(); // 如果使用者註冊過了，就拋出自訂的錯誤
		}
		mailSenderService.verifyRegisterOtp(registerReq.email(), registerReq.otp());// 驗證OTP，錯誤或逾期會拋出InvalidOtpException
		Member member = Member.builder()
				.password(passwordEncoder.encode(registerReq.password()))
				.realName(registerReq.realName())
				.nickName(registerReq.nickName())
				.email(registerReq.email())
				.phone(registerReq.phone())
				.address(registerReq.address())
				.gender(registerReq.gender())
				.createTime(LocalDateTime.now()).build();
		memberRepository.save(member);// 註冊使用者到資料庫
		// 註冊成功的話，就發放JWT給使用者讓其有辦法登入(cookie組裝統一交給jwtUtils處理)
		ResponseCookie cookie = jwtUtils.createTokenCookie(member.getId());
		LoginResp loginResp = LoginResp.builder()
				.email(member.getEmail())
				.nickName(member.getNickName())
				.build();
		return RegisterAndLoginResult.builder().loginResp(loginResp).responseCookie(cookie).build();
	}

	// 獲取某位的資訊
	public MemberInfoResp getMemberInfo(Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		return toMemberInfoResp(member);
	}

	// 登出
	public ResponseCookie logout() { // 把放jwt的coockie設定為已過期的狀態(因為後端無法把客戶端的東西真的刪除掉)
		return ResponseCookie.from("token", "")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.path("/")
				.maxAge(0)
				.build();
	}

	// 更新使用者資訊
	public MemberInfoResp updateMemberInfo(UpdateMemberInfoReq updateMemberInfoReq, Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		objectMapper.updateValue(member, updateMemberInfoReq);// dto有用spring validation檢查過
		// 因為是永續狀態所以不需要用repository save
		return toMemberInfoResp(member);
	}

	// 更新使用者頭像
	public MemberInfoResp updateAvatar(UpdateAvatarReq req, Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		member.setProfileImageUrl(req.profileImageUrl());
		return toMemberInfoResp(member);
	}

	// 把Member組裝成對外回傳的個人資訊DTO，查詢/修改個人資訊/修改頭像都共用這個方法，避免每個方法都重複寫一次builder
	private MemberInfoResp toMemberInfoResp(Member member) {
		return MemberInfoResp.builder()
				.realName(member.getRealName())
				.nickName(member.getNickName())
				.email(member.getEmail())
				.phone(member.getPhone())
				.address(member.getAddress())
				.gender(member.getGender())
				.profileImageUrl(member.getProfileImageUrl())
				.build();
	}

	// 忘記密碼(登入前使用)：mail、新密碼、OTP三者都驗證通過才會真的改密碼
	public void forgotPassword(ForgotPasswordReq forgotPasswordReq) {
		Member member = memberRepository.findByEmail(forgotPasswordReq.email())
				.orElseThrow(() -> new MemberNotFoundException());
		mailSenderService.verifyForgotPasswordOtp(forgotPasswordReq.email(), forgotPasswordReq.otp());// OTP錯誤或逾期會拋出InvalidOtpException
		member.setPassword(passwordEncoder.encode(forgotPasswordReq.newPassword()));// 因為是永續狀態所以不需要用repository save
	}

	public void updatePassword(UpdatePasswordReq updatePasswordReq, Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		// 因為不在意使用者新舊密碼使否相同，新舊密碼是否相同的邏輯在前端檢查，目的是提升使用者體驗(舊密碼使否跟資料庫相同還是有檢查)
		if (!updatePasswordReq.oldPassword().equals(member.getPassword())) {
			throw new InvalidCredentialsException(); // 密碼輸入錯誤
		}
		member.setPassword(passwordEncoder.encode(updatePasswordReq.newPassword()));
	}

	public void deleteMember(DeleteMemberReq deleteMemberReq, Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		if (!passwordEncoder.matches(deleteMemberReq.password(), member.getPassword())) {
			throw new InvalidCredentialsException(); // 密碼輸入錯誤
		}
		memberRepository.delete(member);
	}

	public RegisterAndLoginResult FakeMemberRegisterAndLogin() {
		Member member;
		if (!memberRepository.existsByEmail("JavaJava5241@gmail.com")) {
			// 如果資料庫裡面有真的member，就創建一個
			member = Member.builder()
					.password("GigafixJava520")
					.realName("江村諺")
					.nickName("政寶<3")
					.email("JavaJava5241@gmail.com")
					.phone("0900000000")
					.address("臺北市信義區安康里松勇路66號")
					.gender(Gender.FEMALE)
					.createTime(LocalDateTime.now())
					.profileImageUrl(
							"https://images.unsplash.com/photo-1678105627738-fa7e5ae584f5?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8N3x8amFwYW5lc2UlMjBwb3J0cmFpdHxlbnwwfHwwfHx8MA%3D%3D")
					.build();
			memberRepository.save(member);// 使用hibernate會讓物件變成永續狀態，不需要另外賦值
		} else {
			// 如果資料庫裡面已經有假的member就直接找member
			member = memberRepository.findByEmail("JavaJava5241@gmail.com")
					.orElseThrow(() -> new MemberNotFoundException());
		}
		// 發放JWT(cookie組裝統一交給jwtUtils處理)
		ResponseCookie cookie = jwtUtils.createTokenCookie(member.getId());
		LoginResp loginResp = LoginResp.builder()
				.email(member.getEmail())
				.nickName(member.getNickName())
				.build();
		return RegisterAndLoginResult.builder().loginResp(loginResp).responseCookie(cookie).build();
	}

}
