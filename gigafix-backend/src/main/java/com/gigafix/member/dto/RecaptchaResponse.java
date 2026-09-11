package com.gigafix.member.dto;

// 對應Google reCAPTCHA驗證API回傳的JSON，v2只需要看success這個欄位就夠了
public record RecaptchaResponse(boolean success) {}
