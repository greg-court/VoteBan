package com.codingguru.voteban.util;

public final class MuteStatus {
    private MuteStatus() {}

    public static boolean isActive(boolean muted, long expiry, long now) {
        // Offline Essentials records can retain the flag after expiry. Never extend it.
        return muted && (expiry == 0 || expiry > now);
    }
}
