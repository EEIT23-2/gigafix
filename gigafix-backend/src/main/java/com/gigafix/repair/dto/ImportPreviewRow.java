package com.gigafix.repair.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 匯入預覽：單一列資料的判定結果
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportPreviewRow {

	private String rowLabel; // 給使用者看的識別字串，例如 "id8" 或 "第3筆(新增)"
	private String action; // INSERT / UPDATE / UNCHANGED / ERROR
	private String error; // action=ERROR 時的錯誤訊息
	private List<String> changes; // action=UPDATE 時，每個有變動的欄位一行，例如 "電話：0933333331 → 0911111111"
	private Map<String, String> data; // 正規化後的欄位資料，確認匯入時要送回這份

}
