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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gigafix.repair.dto.ImportResult;
import com.gigafix.repair.dto.StoresRequest;
import com.gigafix.repair.dto.StoresResponse;
import com.gigafix.repair.service.StoresService;
import com.gigafix.repair.util.TableExportImport;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController //(Controller + ResponseBody)
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoresController {
	
	private final StoresService storesServ;
	
//	新增分店：完整的請求網址會是 POST /stores
	@PostMapping
//	不用 @RequestParam，中間多一層DTO
	public ResponseEntity<StoresResponse> insert(@Valid @RequestBody StoresRequest req) {
		StoresResponse res = storesServ.insert(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(res);//201
	}
	
//	修改
	@PutMapping("/{id}")
	public ResponseEntity<StoresResponse> updateById(@PathVariable Byte id, @Valid @RequestBody StoresRequest req) {
		return ResponseEntity.ok(storesServ.updateById(id, req));//200
	}
	
	
//	刪除
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteById(@PathVariable Byte id) {
        storesServ.deleteById(id);
        return ResponseEntity.noContent().build();//204
    }
	
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

//	匯出：format = json / xml / xlsx
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@RequestParam String format) {
		byte[] data = storesServ.export(format);
		return ResponseEntity.ok().headers(TableExportImport.buildDownloadHeaders(format, "stores")).body(data);
	}

//	匯入：依id比對，存在就更新、不存在就新增
	@PostMapping("/import")
	public ResponseEntity<ImportResult> importFile(
			@RequestParam String format, @RequestParam("file") MultipartFile file) {
		return ResponseEntity.ok(storesServ.importFile(file, format));
	}

}
