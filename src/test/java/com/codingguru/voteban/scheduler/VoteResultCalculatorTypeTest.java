package com.codingguru.voteban.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VoteResultCalculatorTypeTest {
    @Test
    void minimumVoteDisplayUsesTheConfiguredCount() {
        assertEquals(7, VoteResultCalculatorType.MIN_VOTES.requiredVotes(7, 23));
    }

    @Test
    void percentageDisplayIsTheFirstCountThatActuallyPasses() {
        var rule = VoteResultCalculatorType.SERVER_PERCENTAGE;
        for (int population = 1; population <= 250; population++) {
            for (int percentage : new int[] {1, 33, 51, 60, 75, 100}) {
                int required = rule.requiredVotes(percentage, population);
                assertTrue(rule.isSuccessful(percentage, required, population));
                if (required > 0) {
                    assertFalse(rule.isSuccessful(percentage, required - 1, population));
                }
            }
        }
    }

    @Test
    void preservesUpstreamPercentageRoundingRatherThanChangingPunishments() {
        var rule = VoteResultCalculatorType.SERVER_PERCENTAGE;
        assertEquals(37, rule.requiredVotes(60, 62));
        assertTrue(rule.isSuccessful(60, 37, 62));
        assertFalse(rule.isSuccessful(60, 36, 62));
    }
}
