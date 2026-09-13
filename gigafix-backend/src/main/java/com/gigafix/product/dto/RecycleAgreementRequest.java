package com.gigafix.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 接收客戶簽署回收同意書時提交的 OTP 與 Canvas 電子簽名。 */
@Getter
@Setter
public class RecycleAgreementRequest {

    @NotBlank(message = "請輸入驗證碼")
    @Pattern(regexp = "\\d{6}", message = "驗證碼必須是 6 位數字")
    private String otp;

    @NotBlank(message = "請完成電子簽名")
    @Size(max = 1_000_000, message = "電子簽名圖片過大")
    private String signatureDataUrl;
}
