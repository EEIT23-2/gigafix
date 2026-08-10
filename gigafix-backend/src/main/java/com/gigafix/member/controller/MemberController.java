package com.gigafix.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.member.dto.LoginReq;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.RegisterReq;
import com.gigafix.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController("gigaFixUsersController")
@RequestMapping("/gigafix/member")
@RequiredArgsConstructor
public class MemberController {
	private final MemberService memberService;
	
	
	@PostMapping("/register")
	public ResponseEntity<LoginResp> register(@Valid @RequestBody RegisterReq registerReq) throws Exception {
		//寄信
		LoginResp loginResp = memberService.register(registerReq);//註冊及登入，所以回傳登入的dto
		return ResponseEntity.status(201).body(loginResp); 
	}
	
	@PostMapping("/login")
	public ResponseEntity<LoginResp> login(@Valid @RequestBody LoginReq loginReq){
		LoginResp loginResp = memberService.login(loginReq);
		return ResponseEntity.status(200).body(loginResp);
	}
	@GetMapping("/test")
	public ResponseEntity<String> login(String email, String password){
		return ResponseEntity.status(200).body(email + password +"登入成功");
	}
	
	
	
}
