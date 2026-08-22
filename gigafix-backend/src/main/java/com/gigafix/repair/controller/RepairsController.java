package com.gigafix.repair.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.repair.dto.AppointmentRequest;
import com.gigafix.repair.dto.RepairsResponse;
import com.gigafix.repair.service.RepairsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/repairs")
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
	public ResponseEntity<RepairsResponse> updateById(@PathVariable Long id, @Valid @RequestBody AppointmentRequest req) {
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
	public ResponseEntity<List<RepairsResponse>> selectAll(){
		return ResponseEntity.ok(rServ.selectAll());//200
	}
	

	
	
	
	
	
	
}
