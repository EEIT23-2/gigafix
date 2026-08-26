package com.gigafix.repair.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.repair.dto.AppointmentRequest;
import com.gigafix.repair.dto.CompleteRepairRequest;
import com.gigafix.repair.dto.InspectionResultRequest;
import com.gigafix.repair.dto.QuotationRequest;
import com.gigafix.repair.dto.RepairsResponse;
import com.gigafix.repair.entity.status.RepairPayStatus;
import com.gigafix.repair.entity.status.RepairStatus;
import com.gigafix.repair.service.RepairsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
public class RepairsController {
	
	private final RepairsService rServ;
	
//	新增
	@PostMapping
	public ResponseEntity<RepairsResponse> insert(@Valid @RequestBody AppointmentRequest req){
		RepairsResponse res = rServ.insert(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(res);//201
	}

	
//	修改
	@PutMapping("/{id}")
	public ResponseEntity<RepairsResponse> updateById(@PathVariable Long id, 
			@Valid @RequestBody AppointmentRequest req) {
		return ResponseEntity.ok(rServ.updateById(id, req));//200
	}
	
//	刪除
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteById(@PathVariable Long id){
		rServ.deleteById(id);
		return ResponseEntity.noContent().build();//204
	}
	
//	id查
	@GetMapping("/{id}")
	public ResponseEntity<RepairsResponse> selectById(@PathVariable Long id){
		return ResponseEntity.ok(rServ.selectById(id));//200
	}
	
//	查全部
	@GetMapping
	public ResponseEntity<List<RepairsResponse>> selectAll(
			@RequestParam(required = false) Long id,
			@RequestParam(required = false) Long memberId,
			@RequestParam(required = false) String memberName,
			@RequestParam(required = false) Integer technicianId,
			@RequestParam(required = false) String technicianName,
			@RequestParam(required = false) RepairStatus status) {
		return ResponseEntity.ok(rServ.search(id, memberId, memberName, technicianId, technicianName, status));//200
	}
	

//	技師查詢：某分店「待估價」且尚未被認領的維修單清單
	@GetMapping("/unassigned")
	public ResponseEntity<List<RepairsResponse>> selectUnassigned(@RequestParam Byte storeId) {
		return ResponseEntity.ok(rServ.selectUnassigned(storeId));//200
	}

//	技師查詢：自己名下的維修單，status可選填
	@GetMapping("/technician/{technicianId}")
	public ResponseEntity<List<RepairsResponse>> selectByTechnician(
			@PathVariable Integer technicianId,
			@RequestParam(required = false) RepairStatus status) {
		return ResponseEntity.ok(rServ.selectByTechnician(technicianId, status));//200
	}

//	技師認領維修單
	@PostMapping("/{id}/assign")
	public ResponseEntity<RepairsResponse> assign(@PathVariable Long id, 
			@RequestParam Integer technicianId) {
		return ResponseEntity.ok(rServ.assign(id, technicianId));//200
	}

//	技師填寫／修改檢測報價（部分更新，還沒送出正式報價前都可以改）
	@PatchMapping("/{id}/quote")
	public ResponseEntity<RepairsResponse> updateQuote(@PathVariable Long id, 
			@Valid @RequestBody QuotationRequest req) {
		return ResponseEntity.ok(rServ.updateQuote(id, req));//200
	}

//	技師正式送出報價：狀態 待估價->已報價，approvalStatus 無->待確認
	@PatchMapping("/{id}/quote/submit")
	public ResponseEntity<RepairsResponse> submitQuote(@PathVariable Long id, 
			@RequestParam Integer technicianId) {
		return ResponseEntity.ok(rServ.submitQuote(id, technicianId));//200
	}
	
//	客戶回應報價（同意／拒絕）
	@PatchMapping("/{id}/approval")
	public ResponseEntity<RepairsResponse> respondToQuote(@PathVariable Long id,
			@RequestParam Long memberId, @RequestParam boolean approve) {
		return ResponseEntity.ok(rServ.respondToQuote(id, memberId, approve));//200
	}
	
//	技師在維修中補充/更新檢測結果
	@PatchMapping("/{id}/inspection-note")
	public ResponseEntity<RepairsResponse> updateInspectionResult(@PathVariable Long id,
			@Valid @RequestBody InspectionResultRequest req) {
		return ResponseEntity.ok(rServ.updateInspectionResult(id, req));//200
	}
	
	
//	技師維修完成
	@PatchMapping("/{id}/complete")
	public ResponseEntity<RepairsResponse> completeRepair(@PathVariable Long id,
			@Valid @RequestBody CompleteRepairRequest req) {
		return ResponseEntity.ok(rServ.completeRepair(id, req));//200
	}
	
//	已通知客戶取件
	@PatchMapping("/{id}/notify")
	public ResponseEntity<RepairsResponse> markNotified(@PathVariable Long id,
			@RequestParam Integer technicianId) {
		return ResponseEntity.ok(rServ.markNotified(id, technicianId));//200
	}
	
//	門市取貨付款、結案
	@PatchMapping("/{id}/close")
	public ResponseEntity<RepairsResponse> closeRepair(@PathVariable Long id,
			@RequestParam Integer technicianId) {
		return ResponseEntity.ok(rServ.closeRepair(id, technicianId));//200
	}
	
//	報價不維修、門市取貨付款(檢測費或0元)、結案
	@PatchMapping("/{id}/closerejected")
	public ResponseEntity<RepairsResponse> closeRejectedRepair(@PathVariable Long id,
			@RequestParam Integer technicianId, @RequestParam(required = false) Integer finalCost) {
		return ResponseEntity.ok(rServ.closeRejectedRepair(id, technicianId, finalCost));//200
	}
	
//	報價後不維修：技師填最終金額(檢測費)送出，狀態推進到尚未取件
	@PatchMapping("/{id}/notify-rejected")
	public ResponseEntity<RepairsResponse> notifyRejected(@PathVariable Long id,
			@RequestParam Integer technicianId, @RequestParam(required = false) Integer finalCost) {
		return ResponseEntity.ok(rServ.notifyRejected(id, technicianId, finalCost));//200
	}
	
//	客戶預約後未送修
	@PatchMapping("/{id}/undelivered")
	public ResponseEntity<RepairsResponse> undelivered(@PathVariable Long id,
			@RequestParam Integer technicianId) {
		return ResponseEntity.ok(rServ.undelivered(id, technicianId));//200
	}
	
//	技師手動更新付款狀態
	@PatchMapping("/{id}/pay-status")
	public ResponseEntity<RepairsResponse> updatePayStatus(@PathVariable Long id,
			@RequestParam RepairPayStatus payStatus) {
		return ResponseEntity.ok(rServ.updatePayStatus(id, payStatus));//200
	}
	
}
