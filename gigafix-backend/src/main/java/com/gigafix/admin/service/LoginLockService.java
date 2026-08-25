package com.gigafix.admin.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.gigafix.admin.security.AdminLoginAttemptInfo;

@Service
public class LoginLockService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    // 全域共用被鎖的Map， key:adminId, value:admin的嘗試當入紀錄；使用ConcurrentHashMap是因為其為thread safe
    private final ConcurrentHashMap<Integer, AdminLoginAttemptInfo> loginAttemptsMap = new ConcurrentHashMap<>();

    //檢查帳號是否還在鎖定中，回傳剩餘鎖定分鐘數，0 代表沒有被鎖
    public long getLockRemainingMinutes(Integer account) {
        AdminLoginAttemptInfo info = loginAttemptsMap.get(account);
        if (info == null || info.getLockedUntil() == null) {
            return 0;
        }

        if (LocalDateTime.now().isAfter(info.getLockedUntil())) {
            // 鎖定時間已過，清除紀錄
            loginAttemptsMap.remove(account);
            return 0;
        }

        return ChronoUnit.MINUTES.between(LocalDateTime.now(), info.getLockedUntil()) + 1;
    }

    //記錄一次登入失敗，回傳目前的錯誤資訊
    public AdminLoginAttemptInfo recordFailedAttempt(Integer account) {
        AdminLoginAttemptInfo info = loginAttemptsMap.computeIfAbsent(account, k -> new AdminLoginAttemptInfo());

        synchronized (info) {
            info.increaseFailCount(); //登入失敗次數++
            if (info.getFailCount() >= MAX_ATTEMPTS) {
                info.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
        }

        return info;
    }

    //登入成功，清除該帳號的錯誤紀錄
    public void resetAttempts(Integer account) {
        loginAttemptsMap.remove(account);
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }
	
}
