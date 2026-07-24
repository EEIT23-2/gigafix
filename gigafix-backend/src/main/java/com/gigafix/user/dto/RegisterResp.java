package com.gigafix.user.dto;

import com.gigafix.user.entity.GigaFixUsers.Gender;

import lombok.Builder;

@Builder //record自帶全參數建構式，所以可以直接使用builder
public record RegisterResp(String realName,
							String nickName, 
							String email, 
							String phone, 
							String address, 
							Gender gender
//							,String token
							) {}
