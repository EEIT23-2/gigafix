package com.gigafix.product.dto;

import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.RecycleStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 對應 recycle-applications-demo.json 中的一筆回收單。
 *
 * 匯入檔只保存會員與門市 ID，不直接反序列化 JPA 關聯實體；
 * Service 會先驗證外鍵，再將這個 DTO 轉換成 RecycleApplication。
 */
@Getter
@Setter
public class RecycleApplicationImportItem {
    /** 回收單所屬會員 ID，匯入前必須確認會員存在。 */
    private Long memberId;
    /** 回收商品名稱。 */
    private String productName;
    /** 商品分類，JSON 使用 IPHONE、WATCH、IPAD 等列舉名稱。 */
    private ProductCategory category;
    /** 商品外觀狀況，例如「9成新」。 */
    private String appearance;
    /** Demo 商品圖片的公開網址。 */
    private String imageUrl;
    /** 商品狀況補充說明。 */
    private String description;
    /** 現場估價；尚未估價時為 null。 */
    private Integer estimatedPrice;
    /** 回收流程狀態，JSON 使用 RecycleStatus 列舉名稱。 */
    private RecycleStatus recycleStatus;
    /** 已簽署案件的 PNG Data URL；未簽署時為 null。 */
    private String agreementSignature;
    /** ISO 8601 格式的簽署時間，未簽署時為 null。 */
    private String agreementSignedTime;
    /** ISO 8601 格式的建立時間。 */
    private String createdTime;
    /** ISO 8601 格式的最後修改時間。 */
    private String lastModifiedTime;
    /** 指定交件門市 ID；未指定門市時為 null。 */
    private Byte storeId;
}
