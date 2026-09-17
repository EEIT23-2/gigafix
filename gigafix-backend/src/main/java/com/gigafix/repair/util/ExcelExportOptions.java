package com.gigafix.repair.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

// Excel 匯出時要套用的保護/防呆設定
@Data
@Builder
public class ExcelExportOptions {

	@Builder.Default
	private Set<String> textColumns = Set.of(); // 強制設成「文字」格式的欄位(避免電話開頭的0被吃掉)

	private String lockedColumn; // 要鎖定不能編輯的欄位(通常是id)，null代表不鎖定、不保護工作表

	@Builder.Default
	private Map<String, List<String>> dropdownColumns = Map.of(); // 欄位名稱 -> 下拉選單可選值清單

	@Builder.Default
	private int extraBlankRows = 0; // 額外預留幾列空白列(套好格式)給使用者新增資料用

}
