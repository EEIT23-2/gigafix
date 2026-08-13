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
import com.gigafix.repair.entity.RepairTechnicians;
import com.gigafix.repair.service.RepairTechniciansService;

import lombok.RequiredArgsConstructor;

@RestController //(Controller + ResponseBody)
@RequestMapping("/repairtechnicians")
@RequiredArgsConstructor
public class RepairTechniciansController {
	
	private final RepairTechniciansService rtServ;
	
	// 新增
    @PostMapping
    public ResponseEntity<RepairTechnicians> insert(@RequestBody RepairTechniciansRequest req) {
        RepairTechnicians rt = rtServ.insert(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(rt);
    }

    // 修改
    @PutMapping("/{id}")
    public ResponseEntity<RepairTechnicians> update(
            @PathVariable Integer id, 
            @RequestBody RepairTechniciansRequest req) {
        RepairTechnicians rt = rtServ.update(id, req);
        return ResponseEntity.ok(rt);
    }

    // 刪除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        rtServ.deleteById(id);
//        操作成功，但不需要回傳任何資料內容
//        .noContent()：指定 HTTP 狀態碼為 204
        return ResponseEntity.noContent().build();
    }

    // id查詢
    @GetMapping("/{id}")
    public ResponseEntity<RepairTechnicians> queryById(@PathVariable Integer id) {
        RepairTechnicians rt = rtServ.selectById(id);
        if (rt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rt);
    }

    // 查詢全部
    @GetMapping
    public ResponseEntity<List<RepairTechnicians>> queryAll() {
        return ResponseEntity.ok(rtServ.selectAll());
    }
}