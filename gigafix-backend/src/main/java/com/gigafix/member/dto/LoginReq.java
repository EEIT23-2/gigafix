package com.gigafix.member.dto;

import lombok.Builder;

@Builder
public record LoginReq(String email,String password) {

}
