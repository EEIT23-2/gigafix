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

	// COLLATE Chinese_Taiwan_Stroke_CI_AS：指定繁體中文筆畫排序定序（不分大小寫、不分腔調），讓分類名稱依中文筆畫順序排序/比較
	// 注意：SQL Server 沒有 Chinese_Taiwan_Stroke_100_CI_AS 這個定序名稱，帶 _100_ 版本的筆畫排序定序全名是 Chinese_Traditional_Stroke_Order_100_CI_AS，寫錯會導致建表失敗
	@Column(name = "name", nullable = false, columnDefinition = "VARCHAR(60) COLLATE Chinese_Taiwan_Stroke_CI_AS")
	private String name;
}
