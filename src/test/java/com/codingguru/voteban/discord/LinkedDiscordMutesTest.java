package com.codingguru.voteban.discord;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LinkedDiscordMutesTest {
    @Test
    void offlineExpiredFlagCannotExtendAMute() {
        long expiry = 1000;
        assertTrue(LinkedDiscordMutes.activeMute(true, expiry, 999));
        assertFalse(LinkedDiscordMutes.activeMute(true, expiry, 1000));
        assertFalse(LinkedDiscordMutes.activeMute(true, expiry, 1001));
        assertFalse(LinkedDiscordMutes.activeMute(false, expiry, 999));
    }

    @Test
    void intentionalPermanentStaffMuteStillApplies() {
        assertTrue(LinkedDiscordMutes.activeMute(true, 0, Long.MAX_VALUE));
        assertFalse(LinkedDiscordMutes.activeMute(false, 0, Long.MAX_VALUE));
    }
}
