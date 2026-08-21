package com.gigafix.repair.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.repair.dto.RepairsRequest;
import com.gigafix.repair.dto.RepairsResponse;
import com.gigafix.repair.entity.Repairs;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.exception.TimeConflictException;
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

	
	private RepairsResponse toResponse(Repairs r) {
		return RepairsResponse.builder()
				.id(r.getId())
				.repairBrand(r.getRepairBrand())
				.repairModel(r.getRepairModel())
				.issueDescription(r.getIssueDescription())
				.bookingDate(r.getBookingDate())
				.timeSlot(r.getTimeSlot())
				.repairStatus(r.getRepairStatus())
				.storeName(r.getStore().getName())
				.build();
	}
	
//	檢查時段衝突
//	Long excludeId:修改時用維修單id排除自己，新增時傳null
	private void checkTimeConflict(Byte storeId, LocalDate bookingDate, LocalTime timeSlot, Long excludeId) {
		// 先 repairs 表查有沒有同分店、同日期、同時段的記錄
		Optional<Repairs> find = rRepos.findByStore_IdAndBookingDateAndTimeSlot(storeId, bookingDate, timeSlot);
		
		// 空的沒人預約
		if(find.isEmpty()) {
			return;
		}
		
		Repairs existing = find.get();
		// excludeId == null：代表現在是新增（insert），不可能是自己，一定要當成衝突
		// existing.getId().equals(excludeId)：代表查到的這筆，id 跟我正在更新的 id 一樣，就是自己
		boolean isMe = (excludeId != null ) && existing.getId().equals(excludeId);
		if(!isMe) {
			throw new TimeConflictException("這個時段已經被預約，請選擇其他時段");
		}
	}
	
//	檢查時段衝突(另一種寫法)
//	private void checkTimeConflict(Byte storeId, LocalDate bookingDate, LocalTime timeSlot, Long excludeId) {
//		rRepos.findByStore_IdAndBookingDateAndTimeSlot(storeId, bookingDate, timeSlot)
//		.filter(r -> excludeId == null || !r.getId().equals(excludeId))
//		.ifPresent(r -> {
//			throw new TimeConflictException("這個時段已經被預約，請選擇其他時段");
//		});
//	}
	
	
//	新增 (查時段是否衝突)
	public RepairsResponse insert(RepairsRequest req) {
//		查得到就代表已被預約
//		使用者體驗：丟出錯誤訊息
		checkTimeConflict(req.getStoreId(), req.getBookingDate(), req.getTimeSlot(), null);
		
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
		
		return toResponse(rRepos.save(repair));
	}
	
//	修改
	public RepairsResponse updateById(Long id, RepairsRequest req) {
		Repairs r = rRepos.findById(id).orElseThrow(() -> new EntityNotFoundException("找不到維修單，id=" + id));
		
		// 排除自己這一筆，不然會誤判成跟自己衝突
		checkTimeConflict(req.getStoreId(), req.getBookingDate(), req.getTimeSlot(), id);
		
		Stores store = sRepos.findById(req.getStoreId())
				.orElseThrow(() -> new EntityNotFoundException("找不到分店，id= " + req.getStoreId()));
		
		r.setStore(store);
		r.setRepairBrand(req.getRepairBrand());
		r.setRepairModel(req.getRepairModel());
		r.setIssueDescription(req.getIssueDescription());
		r.setBookingDate(req.getBookingDate());
		r.setTimeSlot(req.getTimeSlot());
		r.setDropoffType(req.getDropoffType());
		// member 通常送出後不會再改，所以修改這裡沒有
		
		// 這裡不用手動呼叫 save()，因為 repair 是從 rRepos.findById 查出來的，
		// 在 @Transactional 方法內修改它的欄位，交易結束時 Hibernate 會自動偵測到變更並更新（Dirty Checking）
		return toResponse(rRepos.save(r));
	}
	
	
//	刪除
	public void deleteById(Long id) {
		if(!rRepos.existsById(id)) {
			throw new EntityNotFoundException("找不到維修單，id=" + id);
		}
		rRepos.deleteById(id);
	}
	
//	id查
	public RepairsResponse selectById(Long id) {
		Repairs repair =  rRepos.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("找不到維修單，id=" + id));
		return toResponse(repair);
	}
	
//	查全部
	public List<RepairsResponse> selectAll() {

		List<Repairs> list = rRepos.findAll();
		List<RepairsResponse> result = new ArrayList<RepairsResponse>();
		for(Repairs r : list) {
			result.add(toResponse(r));
		}
		return result;
	}
	
//	查全部(另一種寫法)
//	public List<RepairsResponse> findAll() {
//		return rRepos.findAll().stream()
//				.map(this::toResponse)
//				.collect(Collectors.toList());
//	}
	

	
	
	
	
}
