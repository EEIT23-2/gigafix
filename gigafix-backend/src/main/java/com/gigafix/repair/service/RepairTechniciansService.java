package com.gigafix.repair.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
				case "xml" -> tableIO.fromXml(file.getInputStream(), "technician");
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
				String phone = blankToNull(row.get("phone"));
				Byte storeId = parseByteOrNull(row.get("storeId"));
				if (name == null || phone == null || storeId == null) {
					throw new IllegalArgumentException("姓名、電話、分店id都要填寫");
				}
				Stores store = storesRepos.findById(storeId)
						.orElseThrow(() -> new IllegalArgumentException("找不到分店，ID: " + storeId));

				Integer id = parseIntOrNull(row.get("id"));
				RepairTechnicians rt = (id != null) ? rtRepos.findById(id).orElse(null) : null;
				boolean isUpdate = rt != null;
				if (rt == null) {
					rt = new RepairTechnicians();
				}
				rt.setName(name);
				rt.setPhone(phone);
				rt.setStore(store);
				rtRepos.save(rt);
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
