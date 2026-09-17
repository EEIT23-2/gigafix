package com.gigafix.repair.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 匯入結果：新增幾筆、更新幾筆、失敗幾筆(附失敗原因)，回傳給前端顯示
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {

	private int inserted;
	private int updated;
	private int failed;
	private List<String> errors;

}
