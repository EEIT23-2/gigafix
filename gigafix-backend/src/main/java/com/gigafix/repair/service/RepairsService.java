package com.gigafix.repair.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.repair.dto.AppointmentRequest;
import com.gigafix.repair.dto.QuatationRequest;
import com.gigafix.repair.dto.RepairsResponse;
import com.gigafix.repair.entity.RepairTechnicians;
import com.gigafix.repair.entity.Repairs;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.entity.status.ApprovalStatus;
import com.gigafix.repair.entity.status.RepairStatus;
import com.gigafix.repair.exception.InvalidRepairStatusException;
import com.gigafix.repair.exception.TimeConflictException;
import com.gigafix.repair.repository.RepairTechniciansRepository;
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
	private final RepairTechniciansRepository rtRepos;

	
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
				.dropoffType(r.getDropoffType())
//				可能是null的關聯物件」,要往下取欄位前要先判斷
				.technicianId(r.getRepairTechnicians() == null ? null : r.getRepairTechnicians().getId())
				.technicianName(r.getRepairTechnicians() == null ? null : r.getRepairTechnicians().getName())
				.serialNumber(r.getSerialNumber())
				.inspectionResult(r.getInspectionResult())
				.repairItems(r.getRepairItems())
				.estimatedCost(r.getEstimatedCost())
				.approvalStatus(r.getApprovalStatus())
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
	public RepairsResponse insert(AppointmentRequest req) {
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
	public RepairsResponse updateById(Long id, AppointmentRequest req) {
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
	
//	技師查詢：某分店「待估價」且尚未被認領的維修清單
	public List<RepairsResponse> selectUnassigned(Byte storeId) {
		List<Repairs> list = rRepos.findByStore_IdAndRepairStatusAndRepairTechniciansIsNull(storeId, RepairStatus.PENDING_QUOTE);
		List<RepairsResponse> result = new ArrayList<RepairsResponse>();
		for (Repairs r : list) {
			result.add(toResponse(r));
		}
		return result;
	}

//	技師查詢：自己名下的維修單，status可不傳（查全部）或傳入指定狀態
	public List<RepairsResponse> selectByTechnician(Integer technicianId, RepairStatus status) {
		List<Repairs> list;
		if (status != null) {
			list = rRepos.findByRepairTechnicians_IdAndRepairStatus(technicianId, status);
		} else {
			list = rRepos.findByRepairTechnicians_Id(technicianId);
		}
		List<RepairsResponse> result = new ArrayList<RepairsResponse>();
		for (Repairs r : list) {
			result.add(toResponse(r));
		}
		return result;
	}

//	技師認領：維修單必須是「待估價」且尚未被任何技師認領
	public RepairsResponse assign(Long id, Integer technicianId) {
		Repairs r = rRepos.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("找不到維修單，id=" + id));

		if (r.getRepairStatus() != RepairStatus.PENDING_QUOTE) {
//			例如:還沒認領就被客戶退掉/未送檢
			throw new InvalidRepairStatusException("此維修單目前狀態非待估價，無法認領");
		}
		if (r.getRepairTechnicians() != null) {
			throw new InvalidRepairStatusException("此維修單已被其他技師認領");
		}
		
		RepairTechnicians tech = rtRepos.findById(technicianId)
				.orElseThrow(() -> new EntityNotFoundException("找不到技師，id=" + technicianId));

		r.setRepairTechnicians(tech);

		return toResponse(rRepos.save(r));
	}

//	技師填寫、修改檢測報價（可能部分更新）：要先認領，且狀態還是待估價才能修改
	public RepairsResponse updateQuote(Long id, QuatationRequest req) {
		Repairs r = rRepos.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("找不到維修單，id=" + id));

//		null檢查:.getId() 對null呼叫會直接NPE噴錯
		if (r.getRepairTechnicians() == null) {
			throw new InvalidRepairStatusException("尚未有技師認領此維修單，請先認領");
		}
		if (!r.getRepairTechnicians().getId().equals(req.getTechnicianId())) {
		    throw new InvalidRepairStatusException("此維修單不是你認領的，不能修改");
		}
		if (r.getRepairStatus() != RepairStatus.PENDING_QUOTE) {
			throw new InvalidRepairStatusException("此階段無法報價");
		}
		
//		都要判斷null，避免有些資訊沒更新到被null覆蓋
		if (req.getInspectionResult() != null) {
			r.setInspectionResult(req.getInspectionResult());
		}
		if (req.getRepairItems() != null) {
			r.setRepairItems(req.getRepairItems());
		}
		if (req.getEstimatedCost() != null) {
			r.setEstimatedCost(req.getEstimatedCost());
		}

		return toResponse(rRepos.save(r));
	}

//	技師送出報價：repairStatus 待估價->已報價，approvalStatus 無->待確認
	public RepairsResponse submitQuote(Long id, Integer technicianId) {
	    Repairs r = rRepos.findById(id)
	    		.orElseThrow(() -> new EntityNotFoundException("找不到維修單，id=" + id));

	    if (r.getRepairTechnicians() == null) {
	        throw new InvalidRepairStatusException("尚未有技師認領此維修單，請先認領");
	    }
	    if (!r.getRepairTechnicians().getId().equals(technicianId)) {
	        throw new InvalidRepairStatusException("此維修單不是你認領的，不能送出報價");
	    }
	    if (r.getRepairStatus() != RepairStatus.PENDING_QUOTE) {
	        throw new InvalidRepairStatusException("此階段無法報價");
	    }
	    if (r.getInspectionResult() == null || r.getRepairItems() == null || r.getEstimatedCost() == null) {
	        throw new InvalidRepairStatusException("檢測結果、維修項目、估價金額都要填寫完才能送出報價");
	    }

	    r.setRepairStatus(RepairStatus.QUOTED);
	    r.setApprovalStatus(ApprovalStatus.PENDING);

	    return toResponse(rRepos.save(r));
	}
	
	
}
