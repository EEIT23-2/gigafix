package com.gigafix.member.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.gigafix.common.util.JwtUtils;
import com.gigafix.member.dto.UpdatePasswordReq;
import com.gigafix.member.dto.CreateJwtDto;
import com.gigafix.member.dto.ForgotPasswordReq;
import com.gigafix.member.dto.GetMemberInfoResp;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.dto.UpdateMemberInfoReq;
import com.gigafix.member.dto.UpdatedMemberInfoResp;
import com.gigafix.member.dto.DeleteMemberReq;
import com.gigafix.member.entity.Member;
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
		// 註冊成功的話，就發放JWT給使用者讓其有辦法登入
		String jwt = jwtUtils.createToken(CreateJwtDto.builder().subject(String.valueOf(member.getId())).build());
		// 在創建jwt時要把從資料庫撈出來的id轉成字串，避免前端的number型別太小，導致後端的long型別溢位
		ResponseCookie cookie = ResponseCookie.from("token", jwt)
				.httpOnly(true)
				.secure(true)
				.sameSite("None") // 允許跨網域帶cookie
				.path("/")
				.maxAge(Duration.ofMinutes(15))
				.build();// 把JWT放到cookie裡面給controller，之後再放到response header的cookie裡面給前端
		LoginResp loginResp = LoginResp.builder()
				.email(member.getEmail())
				.nickName(member.getNickName())
				.build();
		return RegisterAndLoginResult.builder().loginResp(loginResp).responseCookie(cookie).build();
	}

	// 獲取某位的資訊
	public GetMemberInfoResp getMemberInfo(Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		GetMemberInfoResp getMemberInfoResp = GetMemberInfoResp.builder()
				.realName(member.getRealName())
				.nickName(member.getNickName())
				.email(member.getEmail())
				.phone(member.getPhone())
				.address(member.getAddress())
				.gender(member.getGender()).build();
		return getMemberInfoResp;
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
	public UpdatedMemberInfoResp updateMemberInfo(UpdateMemberInfoReq updateMemberInfoReq, Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		objectMapper.updateValue(member, updateMemberInfoReq);// dto有用spring validation檢查過
		// 因為是永續狀態所以不需要用repository save
		return UpdatedMemberInfoResp.builder()
				.realName(member.getRealName())
				.nickName(member.getNickName())
				.email(member.getEmail())
				.phone(member.getPhone())
				.address(member.getAddress())
				.gender(member.getGender()).build();
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

}
