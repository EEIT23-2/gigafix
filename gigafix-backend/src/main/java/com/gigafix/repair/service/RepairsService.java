package com.gigafix.repair.service;

import org.springframework.stereotype.Service;

import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.repair.dto.RepairsRequest;
import com.gigafix.repair.dto.RepairsResponse;
import com.gigafix.repair.entity.Repairs;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.RepairsRepository;
import com.gigafix.repair.repository.StoresRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RepairsService {
	
	private final RepairsRepository rRepos;
	private final StoresRepository sRepos;
	private final MemberRepository mRepos;
	
//	新增
	public RepairsResponse insert(RepairsRequest req) {
//		查得到就代表已被預約
//		使用者體驗：丟出錯誤訊息
		rRepos.findByStore(req.getStoreId(), req.getBookingDate(), req.getTimeSlot())
		.ifPresent(r -> {
            throw new SlotConflictException("這個時段已經被預約，請選擇其他時段");
		});//還沒寫SlotConflictException
		
		Stores store = sRepos.findById(req.getStoreId())
				.orElseThrow(() -> new EntityNotFoundException("找不到分店"));
		Member member = mRepos.findById(req.getMemberId())
				.orElseThrow(() -> new EntityNotFoundException("找不到會員"));
		Repairs repair = Repairs.builder()
				.member(member)
				.store(store)
				.repairBrand(req.getRepairBrand())
				.repairModel(req.getRepairModel())
				.issueDescription(req.getIssueDescription())
				.bookingDate(req.getBookingDate())
				.timeSlot(req.getTimeSlot())
				.dropoffType(req.getDropoffType())
				.build();
		// repairStatus / estimatedCost / finalCost 有 @Builder.Default，不用手動設
	}
	
	
	
	
	
	
}
