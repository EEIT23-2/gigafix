package com.gigafix.user.dto;

import java.time.LocalDateTime;

import com.gigafix.user.entity.Member.Gender;


public record RegisterReq(String password,
							String realName,
							String nickName, 
							String email, 
							String phone, 
							String address, 
							Gender gender,
							LocalDateTime createDateTime) {}
