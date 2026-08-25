package com.gigafix.admin.security;

import java.time.LocalDateTime;

public class AdminLoginAttemptInfo { //
	private int failCount;
    private LocalDateTime lockedUntil;

    public int getFailCount() {
        return failCount;
    }

    public void increaseFailCount() {
        this.failCount++;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
