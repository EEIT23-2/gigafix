package com.gigafix.forum.util;

import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

/**
 * 文章內文的 HTML 消毒
 *
 * 內文改成富文本（TipTap）之後就是使用者產生的 HTML，而前台是用 v-html 渲染它，
 * 不消毒等於 stored XSS。這一層負責「寫進資料庫的一定是乾淨的」，
 * 前端的 DOMPurify 是第二道防線，負責「既有的髒資料也不會被渲染出來」。
 *
 * 兩邊的允許清單必須一致，否則會出現「編輯器打得出來、存進去卻不見了」的落差。
 * 前端清單見 gigafix-frontend/src/features/forum/htmlContent.js
 */
public final class HtmlSanitizer {

	// 對齊 TipTap StarterKit + Image 實際會產生的標籤。
	// Safelist.relaxed() 已經預設把 a[href] 限制在 ftp/http/https/mailto、img[src] 限制在 http/https，
	// 所以 javascript: 與 data: 本來就進不來，不需要額外設定。
	private static final Safelist SAFELIST = Safelist.relaxed()
			// relaxed() 沒有這幾個：TipTap 的底線、刪除線與水平線
			.addTags("u", "s", "hr")
			// 字級與顏色是 TextStyle 以 <span style="..."> 實作的
			.addTags("span")
			.addAttributes("span", "style")
			// 連結一律補上 rel，避免 target=_blank 的 reverse tabnabbing
			.addEnforcedAttribute("a", "rel", "noopener noreferrer");

	// jsoup 完全不解析 CSS，style 字串會原樣通過——放行 style 之後一定要自己再濾一次，
	// 否則 position:fixed 這類宣告可以做出覆蓋整頁的點擊劫持。
	// 只保留 color / background-color / font-size，其餘宣告一律丟棄。
	//
	// 特別注意：允許的是 background-color 而**不是** background 簡寫——
	// 簡寫可以夾帶 url(...)，而 jsoup 不會去看 CSS 值。
	private static final Pattern SAFE_COLOR = Pattern
			.compile("^(#[0-9a-fA-F]{3,8}|rgba?\\([\\d\\s.,%]+\\)|[a-zA-Z]{3,20})$");

	// 字級收斂成 8~32 的整數 px，與前端 RichTextEditor 的 MIN/MAX_FONT_SIZE 一致。
	// 不放行 em/rem/%：那些單位的實際大小要看上下文，沒辦法用同一組數字界定範圍
	private static final Pattern SAFE_FONT_SIZE = Pattern.compile("^([89]|[12]\\d|3[0-2])px$");

	private HtmlSanitizer() {
	}

	/**
	 * 洗掉不在允許清單內的標籤與屬性，並把 style 屬性收斂到只剩安全的 color/font-size。
	 * null 原樣回傳（草稿允許 null/空字串）。
	 */
	public static String clean(String html) {

		if (html == null || html.isBlank()) {
			return html;
		}
		// 第一段：jsoup 依允許清單清掉標籤與屬性
		Document doc = Jsoup.parseBodyFragment(Jsoup.clean(html, SAFELIST));

		// 第二段：jsoup 管不到 CSS，這裡自己把每個 style 屬性過一遍
		for (Element el : doc.select("[style]")) {
			String filtered = filterStyle(el.attr("style"));
			if (filtered.isEmpty()) {
				el.removeAttr("style");
			} else {
				el.attr("style", filtered);
			}
		}
		return doc.body().html();
	}

	// 只留下 color 與 font-size，且值要通過樣式比對；其餘宣告（position、width、background…）全部丟棄
	private static String filterStyle(String style) {

		StringBuilder kept = new StringBuilder();
		for (String declaration : style.split(";")) {
			int colon = declaration.indexOf(':');
			if (colon < 0) {
				continue;
			}
			String property = declaration.substring(0, colon).trim().toLowerCase();
			String value = declaration.substring(colon + 1).trim();

			boolean ok = ("color".equals(property) && SAFE_COLOR.matcher(value).matches())
					|| ("background-color".equals(property) && SAFE_COLOR.matcher(value).matches())
					|| ("font-size".equals(property) && SAFE_FONT_SIZE.matcher(value).matches());
			if (ok) {
				kept.append(property).append(": ").append(value).append("; ");
			}
		}
		return kept.toString().trim();
	}

	/**
	 * 判斷內文是否為空。
	 *
	 * 不能直接用 isBlank()——TipTap 的空編輯器輸出是 &lt;p&gt;&lt;/p&gt;，
	 * 有長度但沒有內容。只有圖片沒有文字的文章不算空，所以要先看有沒有 img/hr。
	 * 前端有同一套判斷（htmlContent.js 的 isHtmlEmpty），兩邊要一致。
	 */
	public static boolean isEmptyContent(String html) {

		if (html == null || html.isBlank()) {
			return true;
		}
		String lower = html.toLowerCase();
		if (lower.contains("<img") || lower.contains("<hr")) {
			return false;
		}
		// 把標籤剝掉之後看還剩不剩文字；Jsoup 會順便把 &nbsp; 之類的實體解碼
		return Jsoup.parse(html).text().isBlank();
	}
}
