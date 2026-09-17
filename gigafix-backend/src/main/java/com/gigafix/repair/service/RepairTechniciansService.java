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
import com.gigafix.repair.dto.RepairTechniciansRequest;
import com.gigafix.repair.dto.RepairTechniciansResponse;
import com.gigafix.repair.entity.RepairTechnicians;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.exception.DataInUseException;
import com.gigafix.repair.exception.InvalidFileFormatException;
import com.gigafix.repair.exception.RepairNotFoundException;
import com.gigafix.repair.repository.RepairTechniciansRepository;
import com.gigafix.repair.repository.StoresRepository;
import com.gigafix.repair.util.ExcelExportOptions;
import com.gigafix.repair.util.TableExportImport;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RepairTechniciansService {

	private final RepairTechniciansRepository rtRepos;
	//	關聯式 也要帶出 StoresRepository
    private final StoresRepository storesRepos;
    private final TableExportImport tableIO;

	// 匯出匯入固定用這個欄位順序(跟 RepairTechniciansRequest 對應)
    private static final List<String> EXPORT_HEADERS = List.of("id", "name", "phone", "storeId");

    
    private RepairTechniciansResponse toResponse(RepairTechnicians rt) {
        return RepairTechniciansResponse.builder()
                .id(rt.getId())
                .name(rt.getName())
                .phone(rt.getPhone())
                .storeId(rt.getStore().getId())
                .storeName(rt.getStore().getName())
                .storeAddress(rt.getStore().getAddress())
                .storePhone(rt.getStore().getPhone())
                .build();
    }
    

    // 新增：先知道是哪間分店，再新增技師
    public RepairTechniciansResponse insert(RepairTechniciansRequest req) {
    	Stores store = storesRepos.findById(req.getStoreId())
    			.orElseThrow(() -> new RepairNotFoundException("找不到分店，ID: " + req.getStoreId()));
        RepairTechnicians rt = new RepairTechnicians(null, req.getName(), req.getPhone(), store);
        return toResponse(rtRepos.save(rt));
    }

    // 修改：依據技師id去修改姓名、電話、分店
    public RepairTechniciansResponse updateById(Integer id, RepairTechniciansRequest req) {
        // 前端回傳id數字，要先變物件
    	RepairTechnicians rt = rtRepos.findById(id)
                .orElseThrow(() -> new RepairNotFoundException("找不到技師，ID: " + id));
        Stores store = storesRepos.findById(req.getStoreId())
                .orElseThrow(() -> new RepairNotFoundException("找不到分店，ID: " + req.getStoreId()));
        rt.setName(req.getName());
        rt.setPhone(req.getPhone());
        rt.setStore(store);
        return toResponse(rtRepos.save(rt));
    }

    // 刪除
    // controller有回傳狀態碼204，就不用回傳字串提醒刪除成功了
    // 外鍵限制無法刪除
    public void deleteById(Integer id) {
    	if (!rtRepos.existsById(id)) {
            throw new RepairNotFoundException("找不到技師，ID: " + id);
        }
        try {
            rtRepos.deleteById(id);
            rtRepos.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DataInUseException("此技師名下還有維修單記錄，請先處理完畢再刪除");
        }
    }

    // id查詢
    public RepairTechniciansResponse selectById(Integer id) {
    	RepairTechnicians rt = rtRepos.findById(id)
                .orElseThrow(() -> new RepairNotFoundException("找不到技師，ID: " + id));
        return toResponse(rt);
    }

    // 查詢全部技師
    public List<RepairTechniciansResponse> selectAll() {
    	List<RepairTechnicians> list = rtRepos.findAll();
	    List<RepairTechniciansResponse> result = new ArrayList<>();
	    for (RepairTechnicians rt : list) {
	        result.add(toResponse(rt));
	    }
	    return result;
    }
    
	// 查詢：某分店底下的所有技師
    public List<RepairTechniciansResponse> selectByStore(Byte storeId) {
    	List<RepairTechnicians> list = rtRepos.findByStore_Id(storeId);
    	List<RepairTechniciansResponse> result = new ArrayList<>();
    	for (RepairTechnicians rt : list) {
    		result.add(toResponse(rt));
    	}
    	return result;
    }

	private LinkedHashMap<String, String> toRow(RepairTechnicians rt) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("id", rt.getId() == null ? "" : String.valueOf(rt.getId()));
		row.put("name", rt.getName());
		row.put("phone", rt.getPhone());
		row.put("storeId", rt.getStore().getId() == null ? "" : String.valueOf(rt.getStore().getId()));
		return row;
	}

//	匯出：format = json / xml / xlsx
	public byte[] export(String format) {
		List<LinkedHashMap<String, String>> rows = new ArrayList<>();
		for (RepairTechnicians rt : rtRepos.findAll()) {
			rows.add(toRow(rt));
		}
		return switch (format) {
			case "json" -> tableIO.toJson(rows);
			case "xml" -> tableIO.toXml("technicians", "technician", rows);
			case "xlsx" -> tableIO.toExcel(EXPORT_HEADERS, rows, ExcelExportOptions.builder()
					.textColumns(Set.of("phone"))
					.lockedColumn("id")
					.dropdownColumns(Map.of("storeId", storesRepos.findAll().stream()
							.map(s -> String.valueOf(s.getId()))
							.toList()))
					.extraBlankRows(20)
					.build());
			default -> throw new InvalidFileFormatException("不支援的匯出格式: " + format);
		};
	}

	private List<Map<String, String>> parseRows(MultipartFile file, String format) {
		try {
			return switch (format) {
				case "json" -> tableIO.fromJson(file.getInputStream());
				case "xml" -> tableIO.fromXml(file.getInputStream(), "technician");
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
		rawEcho.put("phone", row.get("phone") == null ? "" : row.get("phone").trim());
		rawEcho.put("storeId", row.get("storeId") == null ? "" : row.get("storeId").trim());

		String name = blankToNull(row.get("name"));
		String phone = blankToNull(row.get("phone"));
		Byte storeId = parseByteOrNull(row.get("storeId"));
		if (name == null || phone == null || storeId == null) {
			return ImportPreviewRow.builder().rowLabel(rowLabel).action("ERROR")
					.error("姓名、電話、分店id都要填寫").data(rawEcho).build();
		}
		Stores store = storesRepos.findById(storeId).orElse(null);
		if (store == null) {
			return ImportPreviewRow.builder().rowLabel(rowLabel).action("ERROR")
					.error("找不到 ID " + storeId + " 的分店").data(rawEcho).build();
		}

		Map<String, String> data = new LinkedHashMap<>();
		data.put("id", row.get("id") == null ? "" : row.get("id").trim());
		data.put("name", name);
		data.put("phone", phone);
		data.put("storeId", String.valueOf(storeId));

		Integer id = parseIntOrNull(row.get("id"));
		RepairTechnicians existing = (id != null) ? rtRepos.findById(id).orElse(null) : null;
		if (existing == null) {
			return ImportPreviewRow.builder().rowLabel(rowLabel).action("INSERT").data(data).build();
		}

		List<String> changes = new ArrayList<>();
		if (!name.equals(existing.getName())) {
			changes.add("姓名：" + existing.getName() + " → " + name);
		}
		if (!phone.equals(existing.getPhone())) {
			changes.add("電話：" + existing.getPhone() + " → " + phone);
		}
		if (!storeId.equals(existing.getStore().getId())) {
			changes.add("分店：" + existing.getStore().getName() + " → " + store.getName());
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
						Stores store = storesRepos.findById(Byte.valueOf(evaluated.getData().get("storeId")))
								.orElseThrow(() -> new IllegalArgumentException("找不到分店"));
						RepairTechnicians rt = new RepairTechnicians();
						rt.setName(evaluated.getData().get("name"));
						rt.setPhone(evaluated.getData().get("phone"));
						rt.setStore(store);
						rtRepos.save(rt);
						inserted++;
					}
					case "UPDATE" -> {
						Integer id = parseIntOrNull(evaluated.getData().get("id"));
						RepairTechnicians rt = rtRepos.findById(id)
								.orElseThrow(() -> new IllegalArgumentException("這筆技師已經被刪除，請重新匯入"));
						Stores store = storesRepos.findById(Byte.valueOf(evaluated.getData().get("storeId")))
								.orElseThrow(() -> new IllegalArgumentException("找不到分店"));
						rt.setName(evaluated.getData().get("name"));
						rt.setPhone(evaluated.getData().get("phone"));
						rt.setStore(store);
						rtRepos.save(rt);
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

	private Integer parseIntOrNull(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
