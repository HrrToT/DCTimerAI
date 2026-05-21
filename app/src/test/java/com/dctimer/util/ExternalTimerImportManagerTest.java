package com.dctimer.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExternalTimerImportManagerTest {
    @Test
    public void parsesRealCsTimerExportFromTrainFolder() throws Exception {
        String text = readTrainText("CStimer", "cstimer_20260521_153802.txt");

        ExternalTimerImportManager.ImportBatch batch = ExternalTimerImportManager.parseText(
                ExternalTimerImportManager.SOURCE_CSTIMER, text);

        assertEquals("CSTimer", batch.sourceLabel);
        assertEquals(604, batch.solves.size());
        assertEquals(0, batch.skippedTimeCount);
        assertEquals(0, batch.emptyPenaltyCount);
        assertEquals(0, batch.emptyScrambleCount);
        assertEquals(0, batch.emptyDateCount);
        assertEquals(0, batch.droppedNon333Count);
        assertEquals(0, batch.malformedCount);

        ExternalTimerImportManager.ImportedSolve first = batch.solves.get(0);
        assertEquals(16542, first.timeMs);
        assertEquals(Integer.valueOf(0), first.penalty);
        assertEquals("B2 D2 L B2 L D2 F2 L2 D2 R U2 L D B' L2 U B D2 R U' B'", first.scramble);
        assertTrue(first.sourceTimestamp > 0L);
        assertNotNull(first.originalDate);
        assertTrue(first.originalDate.length() > 0);
    }

    @Test
    public void parsesRealTwistyTimerExportFromTrainFolder() throws Exception {
        String text = readTrainText("Twisty Timer", "Solves_333_三速_21-5月-2026_16-02(1).txt");

        ExternalTimerImportManager.ImportBatch batch = ExternalTimerImportManager.parseText(
                ExternalTimerImportManager.SOURCE_TWISTY_TIMER, text);
        int dnfCount = 0;
        for (ExternalTimerImportManager.ImportedSolve solve : batch.solves) {
            if (solve.penalty != null && solve.penalty == 2) {
                dnfCount++;
            }
        }

        assertEquals("Twisty Timer", batch.sourceLabel);
        assertEquals(700, batch.solves.size());
        assertEquals(0, batch.skippedTimeCount);
        assertEquals(698, batch.emptyPenaltyCount);
        assertEquals(0, batch.emptyScrambleCount);
        assertEquals(0, batch.emptyDateCount);
        assertEquals(0, batch.droppedNon333Count);
        assertEquals(0, batch.malformedCount);
        assertEquals(2, dnfCount);

        ExternalTimerImportManager.ImportedSolve first = batch.solves.get(0);
        assertEquals(13820, first.timeMs);
        assertNull(first.penalty);
        assertEquals("L' B U F' R L U' L2 B' D' L2 B' R2 B' U2 R2 L2 F L2 U2", first.scramble);
        assertTrue(first.sourceTimestamp > 0L);
        assertNotNull(first.originalDate);
        assertTrue(first.originalDate.length() > 0);
    }

    private static String readTrainText(String timerFolder, String fileName) throws IOException {
        File file = resolveTrainFile(timerFolder, fileName);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static File resolveTrainFile(String timerFolder, String fileName) {
        File[] candidates = new File[] {
                new File("train/" + timerFolder + "/" + fileName),
                new File("../train/" + timerFolder + "/" + fileName),
                new File("../../train/" + timerFolder + "/" + fileName)
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Train sample not found for " + timerFolder + ": " + fileName);
    }
}
