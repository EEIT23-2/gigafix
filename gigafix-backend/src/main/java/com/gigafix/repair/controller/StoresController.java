package com.gigafix.repair.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.repair.dto.StoresResponse;
import com.gigafix.repair.service.StoresService;

import lombok.RequiredArgsConstructor;

// 分店查詢，店家地圖(store-locator)、預約選店都會用到，不需要登入
// 新增/修改/刪除/匯出入等後台管理動作見 AdminStoresController
@RestController //(Controller + ResponseBody)
@RequestMapping("/api/gigafix/stores")
@RequiredArgsConstructor
public class StoresController {

	private final StoresService storesServ;

//	id查
	@GetMapping("/{id}")
	public ResponseEntity<StoresResponse> queryById(@PathVariable Byte id) {
        return ResponseEntity.ok(storesServ.selectById(id));//200
    }

//	查全部
	@GetMapping
	public ResponseEntity<List<StoresResponse>> queryAll() {
        return ResponseEntity.ok(storesServ.selectAll());//200
    }

}
