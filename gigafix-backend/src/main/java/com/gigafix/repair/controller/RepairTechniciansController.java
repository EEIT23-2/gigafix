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

import com.gigafix.repair.dto.RepairTechniciansRequest;
import com.gigafix.repair.dto.RepairTechniciansResponse;
import com.gigafix.repair.service.RepairTechniciansService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController //(Controller + ResponseBody)
@RequestMapping("/repairtechnicians")
@RequiredArgsConstructor
public class RepairTechniciansController {
	
	private final RepairTechniciansService rtServ;
	
	// 新增
    @PostMapping
    public ResponseEntity<RepairTechniciansResponse> insert(@Valid @RequestBody RepairTechniciansRequest req) {
        RepairTechniciansResponse res = rtServ.insert(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);//201
    }

    // 修改
    @PutMapping("/{id}")
    public ResponseEntity<RepairTechniciansResponse> updateById(
            @PathVariable Integer id, @Valid @RequestBody RepairTechniciansRequest req) {
        return ResponseEntity.ok(rtServ.updateById(id, req));//200
    }

    // 刪除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        rtServ.deleteById(id);
//        操作成功，但不需要回傳任何資料內容
        return ResponseEntity.noContent().build();//204
    }

    // id查詢
    @GetMapping("/{id}")
    public ResponseEntity<RepairTechniciansResponse> queryById(@PathVariable Integer id) {
        return ResponseEntity.ok(rtServ.selectById(id));//200
    }

    // 查詢全部
    @GetMapping
    public ResponseEntity<List<RepairTechniciansResponse>> queryAll() {
        return ResponseEntity.ok(rtServ.selectAll());//200
    }
}