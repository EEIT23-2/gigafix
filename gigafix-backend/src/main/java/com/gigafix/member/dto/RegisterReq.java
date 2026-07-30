package com.gigafix.member.dto;

import java.time.LocalDateTime;

import com.gigafix.member.entity.Member.Gender;


public record RegisterReq(String password,
							String realName,
							String nickName, 
							String email, 
							String phone, 
							String address, 
							Gender gender,
							LocalDateTime createDateTime) {}
