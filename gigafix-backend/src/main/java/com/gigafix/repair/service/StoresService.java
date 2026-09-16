package com.gigafix.repair.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gigafix.repair.dto.ImportResult;
import com.gigafix.repair.dto.StoresRequest;
import com.gigafix.repair.dto.StoresResponse;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.exception.DataInUseException;
import com.gigafix.repair.exception.InvalidFileFormatException;
import com.gigafix.repair.exception.RepairNotFoundException;
import com.gigafix.repair.repository.StoresRepository;
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
			case "xlsx" -> tableIO.toExcel(EXPORT_HEADERS, rows);
			default -> throw new InvalidFileFormatException("不支援的匯出格式: " + format);
		};
	}

//	匯入：依id比對，資料庫已存在該id就更新、不存在(含id空白)就新增一筆(id由資料庫重新產生)
	public ImportResult importFile(MultipartFile file, String format) {
		List<LinkedHashMap<String, String>> rows;
		try {
			rows = switch (format) {
				case "json" -> tableIO.fromJson(file.getInputStream());
				case "xml" -> tableIO.fromXml(file.getInputStream(), "store");
				case "xlsx" -> tableIO.fromExcel(file.getInputStream(), EXPORT_HEADERS);
				default -> throw new InvalidFileFormatException("不支援的匯入格式: " + format);
			};
		} catch (IOException e) {
			throw new InvalidFileFormatException("無法讀取上傳的檔案", e);
		}

		int inserted = 0;
		int updated = 0;
		List<String> errors = new ArrayList<>();
		int rowNum = 1; // 第1列是標題列，資料從第2列開始
		for (LinkedHashMap<String, String> row : rows) {
			rowNum++;
			try {
				String name = blankToNull(row.get("name"));
				String address = blankToNull(row.get("address"));
				String phone = blankToNull(row.get("phone"));
				if (name == null || address == null || phone == null) {
					throw new IllegalArgumentException("分店名稱、地址、電話都要填寫");
				}

				Byte id = parseByteOrNull(row.get("id"));
				Stores store = (id != null) ? storesRepos.findById(id).orElse(null) : null;
				boolean isUpdate = store != null;
				if (store == null) {
					store = new Stores();
				}
				store.setName(name);
				store.setAddress(address);
				store.setPhone(phone);
				storesRepos.save(store);
				if (isUpdate) {
					updated++;
				} else {
					inserted++;
				}
			} catch (Exception e) {
				errors.add("第" + rowNum + "列：" + e.getMessage());
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
