package com.gigafix.repair.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gigafix.repair.dto.ImportPreview;
import com.gigafix.repair.dto.ImportPreviewRow;
import com.gigafix.repair.dto.ImportResult;
import com.gigafix.repair.dto.StoresRequest;
import com.gigafix.repair.dto.StoresResponse;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.exception.DataInUseException;
import com.gigafix.repair.exception.InvalidFileFormatException;
import com.gigafix.repair.exception.RepairNotFoundException;
import com.gigafix.repair.repository.StoresRepository;
import com.gigafix.repair.util.ExcelExportOptions;
import com.gigafix.repair.util.TableExportImport;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
//1. Lombok 會自動產生帶有 final 欄位的建構子，Spring 會藉此進行依賴注入
public class StoresService {

//2. 宣告為 private final，不用寫 @Autowired
	private final StoresRepository storesRepos;
	private final TableExportImport tableIO;

	// 匯出匯入固定用這個欄位順序(跟 StoresResponse 對應)
	private static final List<String> EXPORT_HEADERS = List.of("id", "name", "address", "phone");

	
	private StoresResponse toResponse(Stores s) {
		return StoresResponse.builder()
				.id(s.getId())
				.name(s.getName())
				.address(s.getAddress())
				.phone(s.getPhone())
				.build();
	}
	
//	新增分店
	public StoresResponse insert(StoresRequest req) {
		Stores store = new Stores();
		store.setName(req.getName());
		store.setAddress(req.getAddress());
		store.setPhone(req.getPhone());
		return toResponse(storesRepos.save(store)); 
	}
	
//	修改
	public StoresResponse updateById(Byte id, StoresRequest req) {
		Stores store = storesRepos.findById(id)
				.orElseThrow(() -> new RepairNotFoundException("分店ID: " + id + " 找不到!"));
		store.setName(req.getName());
		store.setAddress(req.getAddress());
		store.setPhone(req.getPhone());
		return toResponse(storesRepos.save(store)); 
	}
	
//	刪除
//  controller有回傳狀態碼204，就不用回傳字串提醒刪除成功了
	public void deleteById(Byte id) {
		if(!storesRepos.existsById(id)) {
			throw new RepairNotFoundException("分店ID: " + id + " 找不到!");
		}
		try {
			storesRepos.deleteById(id);
			storesRepos.flush();
		} catch (DataIntegrityViolationException e) {
			throw new DataInUseException("此分店底下還有技師或維修單記錄，請先處理完畢再刪除");
		}
	}
	
//	id查詢
	public StoresResponse selectById(Byte id) {
//		lambda 寫法
		Stores store = storesRepos.findById(id)
				.orElseThrow(() -> new RepairNotFoundException("分店ID: " + id + " 找不到!"));
	    return toResponse(store);
	    
//	 // 不用箭頭寫法（if 判斷）
//	    Optional<Stores> op = storesRepos.findById(id);
//	    if (op.isEmpty()) {
//	        throw new RepairNotFoundException("分店ID: " + id + " 找不到!");
//	    }
//	    Stores store = op.get();
	}
	
//	查詢全部
	public List<StoresResponse> selectAll(){
		List<Stores> list = storesRepos.findAll();
	    List<StoresResponse> result = new ArrayList<>();
	    for (Stores s : list) {
	        result.add(toResponse(s));
	    }
	    return result;
	    
//	    //:: 是 Java 8 開始的一個特殊語法，叫做「方法參考」
//		return storesRepos.findAll().stream()
//				.map(this::toResponse)
//                .toList();
	}

	private LinkedHashMap<String, String> toRow(Stores s) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("id", s.getId() == null ? "" : String.valueOf(s.getId()));
		row.put("name", s.getName());
		row.put("address", s.getAddress());
		row.put("phone", s.getPhone());
		return row;
	}

//	匯出：format = json / xml / xlsx
	public byte[] export(String format) {
		List<LinkedHashMap<String, String>> rows = new ArrayList<>();
		for (Stores s : storesRepos.findAll()) {
			rows.add(toRow(s));
		}
		return switch (format) {
			case "json" -> tableIO.toJson(rows);
			case "xml" -> tableIO.toXml("stores", "store", rows);
			case "xlsx" -> tableIO.toExcel(EXPORT_HEADERS, rows, ExcelExportOptions.builder()
					.textColumns(Set.of("phone"))
					.lockedColumn("id")
					.extraBlankRows(20)
					.build());
			default -> throw new InvalidFileFormatException("不支援的匯出格式: " + format);
		};
	}

	private List<Map<String, String>> parseRows(MultipartFile file, String format) {
		try {
			return switch (format) {
				case "json" -> tableIO.fromJson(file.getInputStream());
				case "xml" -> tableIO.fromXml(file.getInputStream(), "store");
				case "xlsx" -> tableIO.fromExcel(file.getInputStream(), EXPORT_HEADERS);
				default -> throw new InvalidFileFormatException("不支援的匯入格式: " + format);
			};
		} catch (IOException e) {
			throw new InvalidFileFormatException("無法讀取上傳的檔案", e);
		}
	}

	private String rowLabel(Map<String, String> row, int position) {
		String rawId = row.get("id");
		return (rawId != null && !rawId.isBlank()) ? "id" + rawId.trim() : "第" + position + "筆(新增)";
	}

//	判斷單一列資料「會新增/會更新(附差異)/沒有變動/錯誤」，不寫入資料庫，預覽跟確認匯入共用同一份邏輯
	private ImportPreviewRow evaluateRow(Map<String, String> row, String rowLabel) {
		// 使用者原始填的內容，就算驗證失敗也要回傳，讓前端預覽時看得出來是哪一筆(而不是只有一個編號)
		Map<String, String> rawEcho = new LinkedHashMap<>();
		rawEcho.put("id", row.get("id") == null ? "" : row.get("id").trim());
		rawEcho.put("name", row.get("name") == null ? "" : row.get("name").trim());
		rawEcho.put("address", row.get("address") == null ? "" : row.get("address").trim());
		rawEcho.put("phone", row.get("phone") == null ? "" : row.get("phone").trim());

		String name = blankToNull(row.get("name"));
		String address = blankToNull(row.get("address"));
		String phone = blankToNull(row.get("phone"));
		if (name == null || address == null || phone == null) {
			return ImportPreviewRow.builder().rowLabel(rowLabel).action("ERROR")
					.error("分店名稱、地址、電話都要填寫").data(rawEcho).build();
		}

		Map<String, String> data = new LinkedHashMap<>();
		data.put("id", row.get("id") == null ? "" : row.get("id").trim());
		data.put("name", name);
		data.put("address", address);
		data.put("phone", phone);

		Byte id = parseByteOrNull(row.get("id"));
		Stores existing = (id != null) ? storesRepos.findById(id).orElse(null) : null;
		if (existing == null) {
			return ImportPreviewRow.builder().rowLabel(rowLabel).action("INSERT").data(data).build();
		}

		List<String> changes = new ArrayList<>();
		if (!name.equals(existing.getName())) {
			changes.add("分店名稱：" + existing.getName() + " → " + name);
		}
		if (!address.equals(existing.getAddress())) {
			changes.add("地址：" + existing.getAddress() + " → " + address);
		}
		if (!phone.equals(existing.getPhone())) {
			changes.add("電話：" + existing.getPhone() + " → " + phone);
		}
		if (changes.isEmpty()) {
			return ImportPreviewRow.builder().rowLabel(rowLabel).action("UNCHANGED").data(data).build();
		}
		return ImportPreviewRow.builder().rowLabel(rowLabel).action("UPDATE").data(data).changes(changes).build();
	}

//	匯入預覽：只解析、比對，不寫入資料庫
	public ImportPreview previewImport(MultipartFile file, String format) {
		List<Map<String, String>> rows = parseRows(file, format);
		List<ImportPreviewRow> result = new ArrayList<>();
		int position = 0;
		for (Map<String, String> row : rows) {
			position++;
			result.add(evaluateRow(row, rowLabel(row, position)));
		}
		return ImportPreview.builder()
				.rows(result)
				.insertCount((int) result.stream().filter(r -> "INSERT".equals(r.getAction())).count())
				.updateCount((int) result.stream().filter(r -> "UPDATE".equals(r.getAction())).count())
				.unchangedCount((int) result.stream().filter(r -> "UNCHANGED".equals(r.getAction())).count())
				.errorCount((int) result.stream().filter(r -> "ERROR".equals(r.getAction())).count())
				.build();
	}

//	確認匯入：前端把預覽時拿到的資料原封不動送回來，這裡重新驗證一次(資料庫可能在預覽後被別人動過)才真的寫入
	public ImportResult confirmImport(List<Map<String, String>> rows) {
		int inserted = 0;
		int updated = 0;
		List<String> errors = new ArrayList<>();
		int position = 0;
		for (Map<String, String> row : rows) {
			position++;
			String label = rowLabel(row, position);
			try {
				ImportPreviewRow evaluated = evaluateRow(row, label);
				switch (evaluated.getAction()) {
					case "ERROR" -> throw new IllegalArgumentException(evaluated.getError());
					case "UNCHANGED" -> {
						// 資料沒變，不用寫回資料庫
					}
					case "INSERT" -> {
						Stores store = new Stores();
						store.setName(evaluated.getData().get("name"));
						store.setAddress(evaluated.getData().get("address"));
						store.setPhone(evaluated.getData().get("phone"));
						storesRepos.save(store);
						inserted++;
					}
					case "UPDATE" -> {
						Byte id = parseByteOrNull(evaluated.getData().get("id"));
						Stores store = storesRepos.findById(id)
								.orElseThrow(() -> new IllegalArgumentException("這筆分店已經被刪除，請重新匯入"));
						store.setName(evaluated.getData().get("name"));
						store.setAddress(evaluated.getData().get("address"));
						store.setPhone(evaluated.getData().get("phone"));
						storesRepos.save(store);
						updated++;
					}
					default -> throw new IllegalStateException("未知的匯入動作: " + evaluated.getAction());
				}
			} catch (Exception e) {
				errors.add(label + "：" + e.getMessage());
			}
		}
		return ImportResult.builder().inserted(inserted).updated(updated).failed(errors.size()).errors(errors).build();
	}

	private String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	private Byte parseByteOrNull(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Byte.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
