package com.dctimer.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class StatsDateSortTest {
    @Test
    public void newestFirstSortsByCompletedDateAndKeepsEmptyDatesLast() {
        String[] dates = {
                "2026-05-01 10:00:00",
                "",
                "2026-05-03 09:00:00",
                "2026-05-03 09:00:00"
        };

        int[] sorted = Stats.buildDateSortedIndex(dates, true);

        assertArrayEquals(new int[] {3, 2, 0, 1}, sorted);
    }

    @Test
    public void oldestFirstTreatsEmptyDatesAsEarliest() {
        String[] dates = {
                "2026-05-01 10:00:00",
                "",
                "2026-05-03 09:00:00",
                "2026-05-03 09:00:00"
        };

        int[] sorted = Stats.buildDateSortedIndex(dates, false);

        assertArrayEquals(new int[] {1, 0, 2, 3}, sorted);
    }
}
