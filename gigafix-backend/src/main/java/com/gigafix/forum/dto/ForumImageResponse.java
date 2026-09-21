package com.gigafix.forum.dto;

/**
 * Response：圖片上傳成功後回傳的網址
 *
 * 只有一個欄位仍然包成物件而不是回裸字串：之後要加欄位（寬高、public_id）才不會破壞前端。
 * 用 record 而不是模組內其他 DTO 那種 Lombok 類別，是因為它沒有 setter 需求也不需要 builder，
 * 寫法比照 common/dto/ErrorResp。
 */
public record ForumImageResponse(String url) {
}
