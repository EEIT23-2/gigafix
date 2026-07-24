package com.gigafix.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.user.dto.RegisterReq;
import com.gigafix.user.dto.RegisterResp;
import com.gigafix.user.entity.GigaFixUsers;
import com.gigafix.user.service.GigaFixUsersService;

import lombok.RequiredArgsConstructor;

@RestController("gigaFixUsersController")
@RequestMapping("/gigafix/users")
@RequiredArgsConstructor
public class GigaFixUsersController {
	private final GigaFixUsersService gigaFixUsersService;
	
	@PostMapping("/register")
	public ResponseEntity<RegisterResp> register(@RequestBody RegisterReq registerReq) throws Exception {
		//寄信
		GigaFixUsers user = gigaFixUsersService.registerGigaFixUser(registerReq);
		//用user的password(??)跟id做JWT
		RegisterResp registerResp = RegisterResp.builder()
						.realName(user.getRealName())
						.nickName(user.getNickName())
						.email(user.getEmail())
						.phone(user.getPhone())
						.address(user.getAddress())
						.gender(user.getGender())
//						.token(jwt)
						.build();
		return ResponseEntity.status(201).body(registerResp); //回傳JWT以及resp
	}
	
	
}
