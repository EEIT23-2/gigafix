package com.gigafix.member.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.member.dto.AdminMemberInfoResp;
import com.gigafix.member.dto.AdminUpdateMemberReq;
import com.gigafix.member.entity.Member;
import com.gigafix.member.exception.MemberNotFoundException;
import com.gigafix.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service @Transactional
@RequiredArgsConstructor
public class AdminMemberService {
	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	//後台：取得所有會員的資訊(不含密碼)
	public List<AdminMemberInfoResp> getAllMembers(){
		return memberRepository.findAll().stream()
				.map(member -> AdminMemberInfoResp.builder()
						.id(member.getId())
						.realName(member.getRealName())
						.nickName(member.getNickName())
						.email(member.getEmail())
						.phone(member.getPhone())
						.address(member.getAddress())
						.gender(member.getGender())
						.createTime(member.getCreateTime()).build())
				.toList();
	}

	//後台：管理員修改指定會員的整包資料(密碼除外)
	public void updateMemberByAdmin(Long id, AdminUpdateMemberReq adminUpdateMemberReq){
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		objectMapper.updateValue(member, adminUpdateMemberReq);//dto有用spring validation檢查過
		//因為是永續狀態所以不需要用repository save
	}

	//後台：管理員刪除指定會員(管理員操作不需要驗證該會員的密碼)
	public void deleteMemberByAdmin(Long id){
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		memberRepository.delete(member);
	}

}
