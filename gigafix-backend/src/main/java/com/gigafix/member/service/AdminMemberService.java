package com.gigafix.member.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gigafix.member.dto.AdminMemberInfoResp;
import com.gigafix.member.dto.AdminUpdateMemberReq;
import com.gigafix.member.dto.MemberMonthlyCountResp;
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

	//後台：分頁取得會員的資訊(不含密碼)，page從0開始
	public Page<AdminMemberInfoResp> getAllMembers(int page, int size){
		Pageable pageable = PageRequest.of(page, size);
		return memberRepository.findAll(pageable)
				.map(member -> AdminMemberInfoResp.builder()
						.id(member.getId())
						.realName(member.getRealName())
						.nickName(member.getNickName())
						.email(member.getEmail())
						.phone(member.getPhone())
						.address(member.getAddress())
						.gender(member.getGender())
						.createTime(member.getCreateTime()).build());
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

	//後台：每月會員註冊數量，依註冊月份分組算當月新增人數(不是累計總數)
	public List<MemberMonthlyCountResp> getMemberMonthlyRegistrations(){
		//TreeMap依YearMonth自然排序，取出來的entrySet本來就是月份由小到大，不用額外再排序
		Map<YearMonth, Long> monthlyNewCount = memberRepository.findAll().stream()
				.filter(member -> member.getCreateTime() != null)
				.collect(Collectors.groupingBy(member -> YearMonth.from(member.getCreateTime()), TreeMap::new, Collectors.counting()));

		DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
		List<MemberMonthlyCountResp> monthlyCounts = new ArrayList<>();
		for (Map.Entry<YearMonth, Long> entry : monthlyNewCount.entrySet()) {
			monthlyCounts.add(MemberMonthlyCountResp.builder()
					.month(entry.getKey().format(monthFormatter))
					.count(entry.getValue())
					.build());
		}
		return monthlyCounts;
	}

}
