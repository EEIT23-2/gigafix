package com.gigafix.common.util;

/**
 * 上傳圖片的格式驗證（共用）
 *
 * 只認檔頭特徵碼（magic number），不看副檔名也不看 Content-Type——
 * 那兩個都是前端說了算，把 .txt 改名成 .png 就能繞過。
 *
 * 刻意做成「純函式 + 回 boolean」而不是自己拋例外：各模組的錯誤格式不一樣
 * （forum 用 ForumException 走 ErrorResp、member 用 InvalidImageFormatException、
 * product 用 ResponseStatusException），把例外留給呼叫端決定才不會綁死。
 *
 * 現況備註：member/controller/MemberController 與 product 的兩支 Service 目前各自還留著
 * 自己的 private 版本，本次沒有一併遷移（那會動到那兩個模組、且頭像的允許格式與這裡不同）。
 * 日後要收斂時，把它們改成呼叫這裡即可。
 */
public final class ImageValidator {

	// 檔頭至少要讀到這麼多 byte 才夠判斷 WebP（RIFF 四碼 + 檔案大小四碼 + WEBP 四碼）
	private static final int MIN_HEADER_LENGTH = 12;

	private ImageValidator() {
		// 純工具類別，不讓人 new
	}

	/**
	 * 是不是支援的圖片格式（JPEG / PNG / GIF / WebP）。
	 *
	 * Java 的 byte 是有號的，0x80 以上會是負數，所以每個位元組都要先 &amp; 0xFF 轉成無號值再比對。
	 */
	public static boolean isSupportedImage(byte[] bytes) {

		if (bytes == null || bytes.length < MIN_HEADER_LENGTH) {
			return false;
		}

		// JPEG：FF D8 FF
		if (matches(bytes, 0, 0xFF, 0xD8, 0xFF)) {
			return true;
		}
		// PNG：89 50 4E 47 0D 0A 1A 0A
		if (matches(bytes, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
			return true;
		}
		// GIF：47 49 46 38（"GIF8"，涵蓋 87a 與 89a 兩種版本）
		if (matches(bytes, 0, 0x47, 0x49, 0x46, 0x38)) {
			return true;
		}
		// WebP：開頭 "RIFF"，第 8~11 個位元組是 "WEBP"（中間四碼是檔案大小，內容不固定）
		if (matches(bytes, 0, 0x52, 0x49, 0x46, 0x46) && matches(bytes, 8, 0x57, 0x45, 0x42, 0x50)) {
			return true;
		}
		return false;
	}

	// 從 offset 開始逐一比對無號位元組；長度不足直接判定不符
	private static boolean matches(byte[] bytes, int offset, int... expected) {

		if (bytes.length < offset + expected.length) {
			return false;
		}
		for (int i = 0; i < expected.length; i++) {
			if ((bytes[offset + i] & 0xFF) != expected[i]) {
				return false;
			}
		}
		return true;
	}
}
