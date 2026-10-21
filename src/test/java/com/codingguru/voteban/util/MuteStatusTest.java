package com.codingguru.voteban.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MuteStatusTest {
    @Test
    void offlineExpiredFlagCannotExtendAMute() {
        long expiry = 1000;
        assertTrue(MuteStatus.isActive(true, expiry, 999));
        assertFalse(MuteStatus.isActive(true, expiry, 1000));
        assertFalse(MuteStatus.isActive(true, expiry, 1001));
        assertFalse(MuteStatus.isActive(false, expiry, 999));
    }

    @Test
    void intentionalPermanentStaffMuteStillApplies() {
        assertTrue(MuteStatus.isActive(true, 0, Long.MAX_VALUE));
        assertFalse(MuteStatus.isActive(false, 0, Long.MAX_VALUE));
    }
}
