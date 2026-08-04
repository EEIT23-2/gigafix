package com.gigafix.member.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.gigafix.member.dto.JwtDto;
import com.gigafix.member.dto.LoginReq;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.util.JwtUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service @Transactional
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepository;
	private final JwtUtils jwtUtils;
	
	public LoginResp register(RegisterReq registerReq) throws Exception {
		Member member = memberRepository.findByEmail(registerReq.email());
		if (member != null) {
			throw new RuntimeException("帳號已註冊");
		}
		member = Member.builder()
				.password(registerReq.password())
				.realName(registerReq.realName())
				.nickName(registerReq.nickName())
				.email(registerReq.email())
				.phone(registerReq.phone())
				.address(registerReq.address())
				.gender(registerReq.gender())
				.createDateTime(LocalDateTime.now()).build();
		memberRepository.save(member);
		LoginResp loginResp = login(LoginReq.builder().email(member.getEmail()).password(member.getPassword()).build());
		return loginResp;
	}
	
	public LoginResp login(LoginReq loginReq) {
		Member member = memberRepository.findByEmail(loginReq.email());
		if (member == null) {
		    throw new RuntimeException("帳號或密碼錯誤");
		}
		if (!loginReq.password().equals(member.getPassword())) {
			throw new RuntimeException("帳號或密碼錯誤");
		}
		
		String jwt = jwtUtils.createToken(JwtDto.builder().subject(member.getId()).username(member.getNickName()).build());
		LoginResp loginResp = LoginResp.builder()
				.realName(member.getRealName())
				.nickName(member.getNickName())
				.email(member.getEmail())
				.phone(member.getPhone())
				.address(member.getAddress())
				.gender(member.getGender())
				.token(jwt)
				.build();
		return loginResp;
	}
	
	
}
