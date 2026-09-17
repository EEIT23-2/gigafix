package com.gigafix.repair.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.common.util.SecurityUtils;
import com.gigafix.repair.dto.AppointmentRequest;
import com.gigafix.repair.dto.PickupPaymentRequest;
import com.gigafix.repair.dto.RepairsResponse;
import com.gigafix.repair.service.RepairsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 會員（登入狀態下）維修單相關動作：預約、查自己的單、回應報價、選取件付款方式
// 後台/技師專用的動作見 AdminRepairsController
@RestController
@RequestMapping("/api/gigafix/repairs")
@RequiredArgsConstructor
public class RepairsController {

	private final RepairsService rServ;

	// 新增（客戶預約，需登入，memberId 從SecurityContextHolder的Authentication解析出來，不是前端傳的）
	@PostMapping("/appointment")
	public ResponseEntity<RepairsResponse> insert(@Valid @RequestBody AppointmentRequest req,
			Authentication authentication) {
		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		RepairsResponse res = rServ.insert(req, memberId);
		return ResponseEntity.status(HttpStatus.CREATED).body(res);// 201
	}

	// 會員中心「維修進度」明細用：只能查自己的維修單，memberId 從SecurityContextHolder的Authentication解析出來
	@GetMapping("/{id}")
	public ResponseEntity<RepairsResponse> selectById(@PathVariable Long id, Authentication authentication) {
		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(rServ.selectByIdForMember(id, memberId));// 200
	}

	// 會員中心「維修進度」用：查登入會員自己的所有維修單，memberId 從SecurityContextHolder的Authentication解析出來
	@GetMapping("/me")
	public ResponseEntity<List<RepairsResponse>> selectByMember(Authentication authentication) {
		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(rServ.selectByMember(memberId));// 200
	}

	// 客戶預約時用：查某分店、某一天已經被預約的時段，前端把這些時段設為不可選；填單流程的一部分，需登入
	@GetMapping("/booked-slots")
	public ResponseEntity<List<LocalTime>> getBookedSlots(
			@RequestParam Byte storeId,
			@RequestParam LocalDate date) {
		return ResponseEntity.ok(rServ.getBookedSlots(storeId, date));// 200
	}

	// 客戶回應報價（同意／拒絕），memberId 從SecurityContextHolder的Authentication解析出來，不是前端傳的
	@PatchMapping("/{id}/approval")
	public ResponseEntity<RepairsResponse> respondToQuote(@PathVariable Long id,
			Authentication authentication, @RequestParam boolean approve) {
		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(rServ.respondToQuote(id, memberId, approve));// 200
	}

	// 客戶選取件方式＋付款方式，只能送出一次，memberId 從SecurityContextHolder的Authentication解析出來
	@PatchMapping("/{id}/pickup-payment")
	public ResponseEntity<RepairsResponse> submitPickupPayment(@PathVariable Long id,
			@Valid @RequestBody PickupPaymentRequest req, Authentication authentication) {
		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		return ResponseEntity.ok(rServ.submitPickupPayment(id, memberId, req));// 200
	}

}
