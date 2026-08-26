package com.gigafix.forum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_id")
	private Integer categoryId;

	// 用 NVARCHAR 而不是 VARCHAR：VARCHAR(n) 的 n 是「位元組數」，中文在 Big5 佔 2 bytes，
	// VARCHAR(60) 實際只放得下約 30 個中文字，會跟 DTO 的 @Size(max = 60)（算字元數）對不起來——
	// 31~60 個中文字會通過驗證卻在寫入時失敗。NVARCHAR(60) 的 60 就是字元數，兩邊才一致。
	// 附帶好處：不再發生 VARCHAR 欄位被隱式轉型成 NVARCHAR 的情況，下面的定序才真的會生效，emoji 也不會變成 ?
	// COLLATE Chinese_Taiwan_Stroke_CI_AS：指定繁體中文筆畫排序定序（不分大小寫、不分腔調），讓分類名稱依中文筆畫順序排序/比較
	// 注意：SQL Server 沒有 Chinese_Taiwan_Stroke_100_CI_AS 這個定序名稱，帶 _100_ 版本的筆畫排序定序全名是 Chinese_Traditional_Stroke_Order_100_CI_AS，寫錯會導致建表失敗
	@Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(60) COLLATE Chinese_Taiwan_Stroke_CI_AS")
	private String name;
}
