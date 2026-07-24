package com.gigafix.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.gigafix.user.dto.RegisterReq;
import com.gigafix.user.dto.RegisterResp;
import com.gigafix.user.entity.GigaFixUsers;
import com.gigafix.user.repository.GigaFixUsersRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service @Transactional
@RequiredArgsConstructor
public class GigaFixUsersService {
	private final GigaFixUsersRepository gigaFixUsersRepository;
//	private final 
	
	public GigaFixUsers registerGigaFixUser(RegisterReq registerReq) throws Exception {
		GigaFixUsers user = gigaFixUsersRepository.findByEmail(registerReq.email());
		if (user != null) {
			throw new RuntimeException();
		}
		user = GigaFixUsers.builder()
				.password(registerReq.password())
				.realName(registerReq.realName())
				.nickName(registerReq.nickName())
				.email(registerReq.email())
				.phone(registerReq.phone())
				.address(registerReq.address())
				.gender(registerReq.gender())
				.createDateTime(LocalDateTime.now()).build();
		return gigaFixUsersRepository.save(user);
	}
	
	
}
