package com.gigafix.member.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import com.gigafix.member.dto.AdminMemberQueryParams;
import com.gigafix.member.dto.AdminUpdateMemberReq;
import com.gigafix.member.dto.MemberMonthlyCountResp;
import com.gigafix.member.entity.Member;
import com.gigafix.member.exception.MemberNotFoundException;
import com.gigafix.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminMemberService {
	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	// 後台：分頁取得會員的資訊(不含密碼)，page從0開始，可依關鍵字/性別/縣市/加入時間區間篩選
	public Page<AdminMemberInfoResp> getAllMembers(AdminMemberQueryParams queryParams) {
		int page = queryParams.page() != null ? queryParams.page() : 0;
		int size = queryParams.size() != null ? queryParams.size() : 20;
		Pageable pageable = PageRequest.of(page, size);

		// 空字串當作沒有篩選，不然JPQL的LIKE %:keyword%會變成比對空字串
		String keyword = (queryParams.keyword() != null && !queryParams.keyword().isBlank())
				? queryParams.keyword().trim()
				: null;
		String city = (queryParams.city() != null && !queryParams.city().isBlank()) ? queryParams.city().trim() : null;

		// 前端只會傳日期(LocalDate)過來，但member.createTime是LocalDateTime(含時分秒)，型別不同沒辦法直接使用
		// 所以要先轉型成LocalDateTime才能拿去跟createTime做區間比對，但會遇到跨日問題，所以要做以下的一些處理
		// atStartOfDay()把LocalDate換成當天00:00:00的LocalDateTime，讓startDate當天一早開始就算在區間內
		LocalDateTime startTime = (queryParams.startDate() != null) ? queryParams.startDate().atStartOfDay() : null;
		// LocalTime.MAX=23:59:59.999999999，讓endDate當天全部時段都算在區間內，不會漏掉當天非0點建立的資料
		LocalDateTime endTime = (queryParams.endDate() != null) ? queryParams.endDate().atTime(LocalTime.MAX) : null;

		return memberRepository.findByConditions(keyword, queryParams.gender(), city, startTime, endTime, pageable)
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

	// 後台：管理員修改指定會員的整包資料(密碼除外)
	public void updateMemberByAdmin(Long id, AdminUpdateMemberReq adminUpdateMemberReq) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		objectMapper.updateValue(member, adminUpdateMemberReq);// dto有用spring validation檢查過
		// 因為是永續狀態所以不需要用repository save
	}

	// 後台：管理員刪除指定會員(管理員操作不需要驗證該會員的密碼)
	public void deleteMemberByAdmin(Long id) {
		Member member = memberRepository.findById(id).orElseThrow(() -> new MemberNotFoundException());
		memberRepository.delete(member);
	}

	// 後台：每月會員註冊數量，依註冊月份分組算當月新增人數(不是累計總數)
	public List<MemberMonthlyCountResp> getMemberMonthlyRegistrations() {
		// TreeMap依YearMonth自然排序，取出來的entrySet本來就是月份由小到大，不用額外再排序
		Map<YearMonth, Long> monthlyNewCount = memberRepository.findAll().stream()
				.filter(member -> member.getCreateTime() != null)
				.collect(Collectors.groupingBy(member -> YearMonth.from(member.getCreateTime()), TreeMap::new,
						Collectors.counting()));

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
