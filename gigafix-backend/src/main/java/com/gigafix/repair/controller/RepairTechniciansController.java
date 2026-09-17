package com.gigafix.repair.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import com.gigafix.repair.dto.ImportPreview;
import com.gigafix.repair.dto.ImportResult;
import com.gigafix.repair.dto.RepairTechniciansRequest;
import com.gigafix.repair.dto.RepairTechniciansResponse;
import com.gigafix.repair.service.RepairTechniciansService;
import com.gigafix.repair.util.TableExportImport;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController //(Controller + ResponseBody)
@RequestMapping("/api/admin/repair/technicians")
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
    public ResponseEntity<List<RepairTechniciansResponse>> queryAll(
    		@RequestParam(required = false) Byte storeId) {
    	if (storeId != null) {
    		return ResponseEntity.ok(rtServ.selectByStore(storeId));//200
    	}
        return ResponseEntity.ok(rtServ.selectAll());//200
    }

    // 匯出：format = json / xml / xlsx
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String format) {
        byte[] data = rtServ.export(format);
        return ResponseEntity.ok().headers(TableExportImport.buildDownloadHeaders(format, "technicians")).body(data);
    }

    // 匯入預覽：只解析、比對，不寫入資料庫
    @PostMapping("/import/preview")
    public ResponseEntity<ImportPreview> previewImport(
            @RequestParam String format, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(rtServ.previewImport(file, format));
    }

    // 確認匯入：把預覽時拿到的資料原封不動送回來，這裡才真的寫入資料庫
    @PostMapping("/import/confirm")
    public ResponseEntity<ImportResult> confirmImport(@RequestBody List<Map<String, String>> rows) {
        return ResponseEntity.ok(rtServ.confirmImport(rows));
    }
}