package com.gigafix.admin.security;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component
public class SuperAdminSetupState {
	private final AtomicBoolean setupAttempted = new AtomicBoolean(false); //型別的Boolean是threads save的型別

    public boolean tryLock() {
        return setupAttempted.compareAndSet(false, true);
    }

    public void reset() {
        setupAttempted.set(false);
    }
}
