package com.gigafix.repair.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 匯入預覽：整份檔案的判定結果，只解析驗證、還沒寫入資料庫
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportPreview {

	private List<ImportPreviewRow> rows;
	private int insertCount;
	private int updateCount;
	private int unchangedCount;
	private int errorCount;

}
