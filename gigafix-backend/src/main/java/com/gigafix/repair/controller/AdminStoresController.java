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
import com.gigafix.repair.dto.StoresRequest;
import com.gigafix.repair.dto.StoresResponse;
import com.gigafix.repair.service.StoresService;
import com.gigafix.repair.util.TableExportImport;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 分店後台管理：新增/修改/刪除/匯出入
// 公開查詢（店家地圖、預約選店）見 StoresController
@RestController //(Controller + ResponseBody)
@RequestMapping("/api/admin/repair/stores")
@RequiredArgsConstructor
public class AdminStoresController {

	private final StoresService storesServ;

//	新增分店
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

//	匯出：format = json / xml / xlsx
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@RequestParam String format) {
		byte[] data = storesServ.export(format);
		return ResponseEntity.ok().headers(TableExportImport.buildDownloadHeaders(format, "stores")).body(data);
	}

//	匯入預覽：只解析、比對，不寫入資料庫
	@PostMapping("/import/preview")
	public ResponseEntity<ImportPreview> previewImport(
			@RequestParam String format, @RequestParam("file") MultipartFile file) {
		return ResponseEntity.ok(storesServ.previewImport(file, format));
	}

//	確認匯入：把預覽時拿到的資料原封不動送回來，這裡才真的寫入資料庫
	@PostMapping("/import/confirm")
	public ResponseEntity<ImportResult> confirmImport(@RequestBody List<Map<String, String>> rows) {
		return ResponseEntity.ok(storesServ.confirmImport(rows));
	}

}
