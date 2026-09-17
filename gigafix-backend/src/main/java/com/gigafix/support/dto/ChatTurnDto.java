package com.gigafix.support.dto;

// 前端帶回的單輪對話紀錄，伺服器不存這些內容，只用來組出這次要送給 Groq 的上下文
public record ChatTurnDto(String role, String content) {
}
