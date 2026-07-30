package com.gigafix.member.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service @Transactional
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepository;
//	private final 
	
	public Member registerGigaFixUser(RegisterReq registerReq) throws Exception {
		Member user = memberRepository.findByEmail(registerReq.email());
		if (user != null) {
			throw new RuntimeException();
		}
		user = Member.builder()
				.password(registerReq.password())
				.realName(registerReq.realName())
				.nickName(registerReq.nickName())
				.email(registerReq.email())
				.phone(registerReq.phone())
				.address(registerReq.address())
				.gender(registerReq.gender())
				.createDateTime(LocalDateTime.now()).build();
		return memberRepository.save(user);
	}
	
	
}
