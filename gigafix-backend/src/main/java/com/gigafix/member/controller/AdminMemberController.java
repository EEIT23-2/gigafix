package com.gigafix.member.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.member.dto.AdminMemberInfoResp;
import com.gigafix.member.dto.AdminUpdateMemberReq;
import com.gigafix.member.service.AdminMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {
	private final AdminMemberService adminMemberService;

	@GetMapping //取得所有會員的資訊(不含密碼)
	public ResponseEntity<List<AdminMemberInfoResp>> getAllMembers(){
		List<AdminMemberInfoResp> members = adminMemberService.getAllMembers();
		return ResponseEntity.ok(members);
	}

	@PutMapping("/{memberId}") //管理員修改指定會員的整包資料(前端要把該會員整包資料傳回來)
	public ResponseEntity<Void> updateMember(@PathVariable Long memberId, @Valid @RequestBody AdminUpdateMemberReq adminUpdateMemberReq){
		adminMemberService.updateMemberByAdmin(memberId, adminUpdateMemberReq);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{memberId}") //管理員刪除指定會員
	public ResponseEntity<Void> deleteMember(@PathVariable Long memberId){
		adminMemberService.deleteMemberByAdmin(memberId);
		return ResponseEntity.noContent().build();
	}

}
