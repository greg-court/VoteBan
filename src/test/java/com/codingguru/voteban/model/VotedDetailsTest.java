package com.codingguru.voteban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class VotedDetailsTest {
    private final VotedDetails details = new VotedDetails(222_000, null);

    @Test
    void formatsTheExistingExpiryAndRoundsPartialSecondsUp() {
        assertEquals("3m 42s", details.getRemainingTime(0));
        assertEquals("3m 42s", details.getRemainingTime(1));
        assertEquals("1m 0s", details.getRemainingTime(162_001));
        assertEquals("59s", details.getRemainingTime(163_000));
        assertEquals("1s", details.getRemainingTime(221_999));
    }

    @Test
    void expiredCooldownsNeverDisplayNegativeTime() {
        assertEquals("0s", details.getRemainingTime(222_000));
        assertEquals("0s", details.getRemainingTime(222_001));
        assertEquals("0s", details.getRemainingTime(300_000));
    }
}
