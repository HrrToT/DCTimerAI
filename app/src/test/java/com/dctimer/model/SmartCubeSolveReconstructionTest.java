package com.dctimer.model;

import com.dctimer.APP;
import com.dctimer.util.Utils;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SmartCubeSolveReconstructionTest {
    private static final String SOLVED = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";

    @Test
    public void mergesAdjacentSameFaceTurnsIntoOneMove() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(3, 0, 0));
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(3, 80, 80));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

        assertEquals("R2", reconstruction.getMoveSequence());
        assertEquals(1, reconstruction.getMoveCount());
    }

    @Test
    public void wrapsOppositeLayerTurnsAsSliceDisplayText() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(0, 0, 0));
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(11, 90, 90));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

        assertEquals("E", reconstruction.getMoveSequence());
        assertEquals("U D'", reconstruction.getPhysicalMoveSequence());
        assertEquals(1, reconstruction.getMoveCount());
    }

    @Test
    public void wrapsOppositeLayerTurnsAsSliceWithoutTimeWindow() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(0, 0, 0));
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(11, 400, 400));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

        assertEquals("E", reconstruction.getMoveSequence());
        assertEquals("U D'", reconstruction.getPhysicalMoveSequence());
        assertEquals(1, reconstruction.getMoveCount());
    }

    @Test
    public void doesNotWrapNonConsecutiveOppositeLayerTurns() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(0, 0, 0));
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(3, 90, 90));
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(11, 180, 180));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

        assertEquals("U R D'", reconstruction.getMoveSequence());
        assertEquals("U R D'", reconstruction.getPhysicalMoveSequence());
        assertEquals(3, reconstruction.getMoveCount());
    }

    @Test
    public void keepsFinalAufTurnsInsidePllPhase() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(0, 0, 0));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

        assertTrue(reconstruction.getPrettySolve().contains("// PLL"));
        assertTrue(!reconstruction.getPrettySolve().contains("// AUF"));
    }

    @Test
    public void emitsPhaseMetadataJson() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(3, 0, 0));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

        assertTrue(reconstruction.toJson(1000).contains("\"method\":\"333-smart-cf4op\""));
        assertTrue(reconstruction.toJson(1000).contains("\"physicalMoves\":\"R\""));
        assertTrue(reconstruction.toJson(1000).contains("\"displayMoves\":\"R\""));
        assertTrue(reconstruction.toJson(1000).contains("\"displaySteps\""));
    }

    @Test
    public void displayStepCarriesPhysicalMovesForSliceReplay() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(0, 80, 80));
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(11, 90, 170));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);
        String json = reconstruction.toJson(1000);

        assertTrue(json.contains("\"notation\":\"E\""));
        assertTrue(json.contains("\"physicalMoves\":[\"U\",\"D'\"]"));
        assertTrue(json.contains("\"physicalMoveCount\":2"));
    }

    @Test
    public void doesNotWrapSliceAcrossCfopPhaseBoundary() throws Exception {
        Class<?> prettyMoveClass = Class.forName("com.dctimer.model.SmartCubeSolveReconstruction$PrettyMove");
        Constructor<?> ctor = prettyMoveClass.getDeclaredConstructor(int.class, int.class, int.class,
                int.class, int.class, int.class, int.class);
        ctor.setAccessible(true);
        List<Object> physicalMoves = new ArrayList<>();
        physicalMoves.add(ctor.newInstance(0, 0, 0, 0, 80, 80, 0));
        physicalMoves.add(ctor.newInstance(3, 2, 1, 1, 170, 170, 1));

        Method method = SmartCubeSolveReconstruction.class.getDeclaredMethod("buildReplaySteps", List.class);
        method.setAccessible(true);
        List<?> replaySteps = (List<?>) method.invoke(null, physicalMoves);

        assertEquals(2, replaySteps.size());
    }

    @Test
    public void prettySolveOmitsPhaseMoveCountsAndAppendsSolveStats() {
        List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
        raw.add(new SmartCubeSolveReconstruction.MoveEvent(3, 0, 0));

        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);
        String prettySolve = reconstruction.getPrettySolve(1000);

        assertTrue(prettySolve.contains("Total: 1 moves"));
        assertTrue(prettySolve.contains("TPS: 1.0 tps"));
        assertTrue(!prettySolve.contains("move(s)"));
    }

    @Test
    public void cf4opProgressDoesNotCountOneSolvedF2lSlotAsEverySlot() throws Exception {
        char[] facelets = SOLVED.toCharArray();
        facelets[21] = 'B';
        facelets[14] = 'F';
        facelets[39] = 'B';

        assertEquals(5, invokeCf4opProgress(new String(facelets)));
    }

    @Test
    public void customOrientationChangesDisplayedMovesWithoutChangingPhaseDetection() {
        int originalOrientation = APP.smartCubeSolveOrientation;
        try {
            APP.smartCubeSolveOrientation = findOrientation(3, 2);
            List<SmartCubeSolveReconstruction.MoveEvent> raw = new ArrayList<>();
            raw.add(new SmartCubeSolveReconstruction.MoveEvent(3, 0, 0));
            raw.add(new SmartCubeSolveReconstruction.MoveEvent(0, 120, 120));

            SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(SOLVED, raw);

            assertEquals("L D", reconstruction.getMoveSequence());
            assertTrue(reconstruction.getPrettySolve().contains("L D // PLL"));
        } finally {
            APP.smartCubeSolveOrientation = originalOrientation;
        }
    }

    private static int invokeCf4opProgress(String facelets) throws Exception {
        Method method = SmartCubeSolveReconstruction.class.getDeclaredMethod("getCf4opProgress", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(null, facelets);
    }

    private static int findOrientation(int top, int front) {
        for (int i = 0; i < Utils.SMART_CUBE_ORIENTATION_FACES.length; i++) {
            int[] pair = Utils.SMART_CUBE_ORIENTATION_FACES[i];
            if (pair[0] == top && pair[1] == front) {
                return i;
            }
        }
        return 0;
    }

}
