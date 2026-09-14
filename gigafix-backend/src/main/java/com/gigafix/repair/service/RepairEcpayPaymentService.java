package com.gigafix.repair.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.gigafix.repair.entity.Repairs;
import com.gigafix.repair.entity.status.RepairPay;
import com.gigafix.repair.entity.status.RepairPayStatus;
import com.gigafix.repair.exception.InvalidRepairStatusException;
import com.gigafix.repair.exception.NotEligibleException;
import com.gigafix.repair.exception.RepairNotFoundException;
import com.gigafix.repair.repository.RepairsRepository;

import lombok.RequiredArgsConstructor;

// repair 模組自己獨立的綠界金流串接，不依賴 order 模組的 EcpayPaymentService，
// 只共用 application.properties 裡的 MerchantID/HashKey/HashIV/付款頁網址設定值
@Service
@RequiredArgsConstructor
public class RepairEcpayPaymentService {

	private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
	private static final DateTimeFormatter TRADE_DATE_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy/MM/dd HH:mm:ss");
	private static final DateTimeFormatter TRADE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
	private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final RepairsRepository repairsRepository;
	private final RepairsService repairsService;

	@Value("${ecpay.payment.merchant-id}")
	private String merchantId;

	@Value("${ecpay.payment.hash-key}")
	private String hashKey;

	@Value("${ecpay.payment.hash-iv}")
	private String hashIv;

	@Value("${ecpay.payment.url}")
	private String paymentUrl;

	@Value("${ecpay.repair-payment.return-url}")
	private String returnUrl;

	@Value("${ecpay.repair-payment.result-url}")
	private String resultUrl;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	/**
	 * 客戶按「線上付款」送出後呼叫，Controller直接把這段HTML回傳給瀏覽器，
	 * 瀏覽器會自動 POST 到綠界付款頁
	 */
	public String buildPaymentHtml(Long memberId, Long repairId) {
		Repairs repair = getPayableRepair(memberId, repairId);
		Map<String, String> parameters = buildPaymentParameters(repair);
		parameters.put("CheckMacValue", calculateCheckMacValue(parameters));
		return buildAutoSubmitHtml(parameters);
	}

	private Repairs getPayableRepair(Long memberId, Long repairId) {
		Repairs repair = repairsRepository.findById(repairId)
				.orElseThrow(() -> new RepairNotFoundException("找不到維修單，id=" + repairId));

		if (!repair.getMember().getId().equals(memberId)) {
			throw new NotEligibleException("此維修單不是你的，無法付款");
		}
		if (repair.getRepairPay() != RepairPay.ONLINE) {
			throw new InvalidRepairStatusException("此維修單未選擇線上付款");
		}
		if (repair.getRepairPayStatus() == RepairPayStatus.PAID) {
			throw new InvalidRepairStatusException("此維修單已完成付款");
		}
		if (repair.getFinalCost() == null || repair.getFinalCost() <= 0) {
			throw new InvalidRepairStatusException("維修費用金額不正確");
		}

		return repair;
	}

	private Map<String, String> buildPaymentParameters(Repairs repair) {
		Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		parameters.put("MerchantID", merchantId);
		parameters.put("MerchantTradeNo", generateMerchantTradeNo());
		parameters.put("MerchantTradeDate", LocalDateTime.now(TAIPEI_ZONE).format(TRADE_DATE_FORMATTER));
		parameters.put("PaymentType", "aio");
		parameters.put("TotalAmount", String.valueOf(repair.getFinalCost()));
		parameters.put("TradeDesc", "GigaFix Repair Payment");
		parameters.put("ItemName", buildItemName(repair));
		parameters.put("ReturnURL", returnUrl);
		parameters.put("ChoosePayment", "Credit");
		parameters.put("EncryptType", "1");
		parameters.put("OrderResultURL", resultUrl);
		parameters.put("NeedExtraPaidInfo", "N");

		// 用來在 Callback 找回 GigaFix 維修單
		parameters.put("CustomField1", String.valueOf(repair.getId()));
		return parameters;
	}

	/**
	 * 產生不超過 20 字元且只包含英數字的 MerchantTradeNo
	 * GR + yyMMddHHmmss + 6 位亂數 = 20 字元(GR跟訂單那邊的GF開頭區分開)
	 */
	private String generateMerchantTradeNo() {
		String timePart = LocalDateTime.now(TAIPEI_ZONE).format(TRADE_NO_TIME_FORMATTER);
		StringBuilder randomPart = new StringBuilder();

		for (int i = 0; i < 6; i++) {
			randomPart.append(RANDOM_CHARS.charAt(SECURE_RANDOM.nextInt(RANDOM_CHARS.length())));
		}

		return "GR" + timePart + randomPart;
	}

	// ECPay 用 "#" 當作多筆商品的分隔符號，畫面上會拆成一行一個，不是普通文字
	private String buildItemName(Repairs repair) {
		String repairLine = "維修單編號" + repair.getId();
		String modelLine = repair.getRepairBrand() + " " + repair.getRepairModel();
		return repairLine + "#" + modelLine;
	}

	/**
	 * 產生自動 POST 到 ECPay 的 HTML
	 */
	private String buildAutoSubmitHtml(Map<String, String> parameters) {
		StringBuilder html = new StringBuilder();
		html.append("""
				<!DOCTYPE html>
				<html lang="zh-Hant">
				<head>
				    <meta charset="UTF-8">
				    <title>前往綠界付款</title>
				</head>
				<body>
				    <p>正在前往綠界安全付款頁面...</p>
				    <form id="ecpayRepairPaymentForm" method="post" action="%s">
				""".formatted(HtmlUtils.htmlEscape(paymentUrl)));

		parameters.forEach((name, value) -> appendHiddenInput(html, name, value));

		html.append("""
				    </form>
				    <script>
				        document.getElementById('ecpayRepairPaymentForm').submit();
				    </script>
				</body>
				</html>
				""");

		return html.toString();
	}

	private void appendHiddenInput(StringBuilder html, String name, String value) {
		html.append("""
				<input type="hidden" name="%s" value="%s">
				""".formatted(HtmlUtils.htmlEscape(name), HtmlUtils.htmlEscape(value)));
	}

	/**
	 * ReturnURL：綠界伺服器對伺服器回調，用來真正確認付款完成、更新維修單付款狀態
	 */
	public void processPaymentReturn(Map<String, String> callback) {
		validateCallbackSource(callback);

		String rtnCode = requireCallbackValue(callback, "RtnCode");
		if (!"1".equals(rtnCode)) {
			return;
		}

		// Stage 測試站的「模擬付款通知」，不是消費者真正完成付款，不更新狀態
		if ("1".equals(callback.get("SimulatePaid"))) {
			return;
		}

		Long repairId = parseRepairId(requireCallbackValue(callback, "CustomField1"));
		int tradeAmount = parseTradeAmount(requireCallbackValue(callback, "TradeAmt"));

		Repairs repair = repairsRepository.findById(repairId)
				.orElseThrow(() -> new RepairNotFoundException("找不到維修單，id=" + repairId));

		if (repair.getFinalCost() == null || repair.getFinalCost().intValue() != tradeAmount) {
			throw new InvalidRepairStatusException("ECPay 付款金額與維修單金額不一致");
		}

		repairsService.updatePayStatus(repairId, RepairPayStatus.PAID);
	}

	private void validateCallbackSource(Map<String, String> callback) {
		String receivedCheckMacValue = requireCallbackValue(callback, "CheckMacValue");
		String expectedCheckMacValue = calculateCheckMacValue(callback);

		if (!secureEquals(expectedCheckMacValue, receivedCheckMacValue)) {
			throw new IllegalArgumentException("ECPay CheckMacValue 驗證失敗");
		}

		String callbackMerchantId = requireCallbackValue(callback, "MerchantID");
		if (!merchantId.equals(callbackMerchantId)) {
			throw new IllegalArgumentException("ECPay MerchantID 不正確");
		}
	}

	private String requireCallbackValue(Map<String, String> callback, String key) {
		String value = callback.get(key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("ECPay Callback 缺少參數：" + key);
		}

		return value.trim();
	}

	private Long parseRepairId(String value) {
		try {
			long repairId = Long.parseLong(value);
			if (repairId <= 0) {
				throw new NumberFormatException();
			}

			return repairId;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("ECPay CustomField1 的 repairId 不正確");
		}
	}

	private int parseTradeAmount(String value) {
		try {
			int amount = Integer.parseInt(value);
			if (amount <= 0) {
				throw new NumberFormatException();
			}

			return amount;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("ECPay TradeAmt 不正確");
		}
	}

	private boolean secureEquals(String expected, String received) {
		byte[] expectedBytes = expected.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
		byte[] receivedBytes = received.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
		return MessageDigest.isEqual(expectedBytes, receivedBytes);
	}

	/**
	 * ECPay AioCheckOut CheckMacValue
	 */
	public String calculateCheckMacValue(Map<String, String> sourceParameters) {
		Map<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		sourceParameters.forEach((key, value) -> {
			if (!"CheckMacValue".equalsIgnoreCase(key)) {
				sorted.put(key, value == null ? "" : value);
			}
		});

		StringJoiner joiner = new StringJoiner("&");
		sorted.forEach((key, value) -> joiner.add(key + "=" + value));

		String raw = "HashKey=" + hashKey + "&" + joiner + "&HashIV=" + hashIv;
		String encoded = ecpayUrlEncode(raw).toLowerCase(Locale.ROOT);
		return sha256(encoded).toUpperCase(Locale.ROOT);
	}

	private String ecpayUrlEncode(String value) {
		String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
		return encoded
				.replace("%2D", "-")
				.replace("%5F", "_")
				.replace("%2E", ".")
				.replace("%21", "!")
				.replace("%2A", "*")
				.replace("%28", "(")
				.replace("%29", ")");
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder();

			for (byte b : hash) {
				result.append(String.format("%02x", b & 0xff));
			}

			return result.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 不可用", e);
		}
	}

	/**
	 * OrderResultURL：瀏覽器端付款結果回傳。跟order模組的作法一致，
	 * demo環境ReturnURL/OrderResultURL常常打在同一個對外網址(ngrok)上，
	 * 這裡也順便呼叫processPaymentReturn確保狀態一定會更新一次；
	 * 真正權威的付款完成判斷仍然是ReturnURL。
	 */
	public String processPaymentResult(Map<String, String> callback) {
		processPaymentReturn(callback);

		Long repairId = parseRepairId(requireCallbackValue(callback, "CustomField1"));

		return UriComponentsBuilder
				.fromUriString(frontendBaseUrl)
				.path("/member-center/repair/{repairId}")
				.queryParam("payment", "result")
				.buildAndExpand(repairId)
				.encode()
				.toUriString();
	}
}
