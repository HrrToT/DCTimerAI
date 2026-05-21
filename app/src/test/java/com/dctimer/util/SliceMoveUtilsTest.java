package com.dctimer.util;

import org.junit.Test;

import cs.min2phase.CubieCube;
import cs.min2phase.Util;

import static org.junit.Assert.assertEquals;

public class SliceMoveUtilsTest {
    private static final String SOLVED = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";

    @Test
    public void eThenInverseReturnsSolved() {
        assertReturnsSolved(18, 20);
    }

    @Test
    public void mThenInverseReturnsSolved() {
        assertReturnsSolved(21, 23);
    }

    @Test
    public void sThenInverseReturnsSolved() {
        assertReturnsSolved(24, 26);
    }

    @Test
    public void eRepeatedFourTimesReturnsSolved() {
        assertFourTurnsReturnSolved(18);
    }

    @Test
    public void mRepeatedFourTimesReturnsSolved() {
        assertFourTurnsReturnSolved(21);
    }

    @Test
    public void sRepeatedFourTimesReturnsSolved() {
        assertFourTurnsReturnSolved(24);
    }

    @Test
    public void sliceMovesKeepFaceCentersFixed() {
        assertCentersFixed(18);
        assertCentersFixed(21);
        assertCentersFixed(24);
    }

    private void assertReturnsSolved(int move, int inverse) {
        CubieCube cube = new CubieCube();
        cube = SliceMoveUtils.applyMove(cube, move);
        cube = SliceMoveUtils.applyMove(cube, inverse);
        assertEquals(SOLVED, Util.toFaceCube(cube));
    }

    private void assertFourTurnsReturnSolved(int move) {
        CubieCube cube = new CubieCube();
        for (int i = 0; i < 4; i++) {
            cube = SliceMoveUtils.applyMove(cube, move);
        }
        assertEquals(SOLVED, Util.toFaceCube(cube));
    }

    private void assertCentersFixed(int move) {
        CubieCube cube = new CubieCube();
        cube = SliceMoveUtils.applyMove(cube, move);
        String facelets = Util.toFaceCube(cube);
        int[] centerIndices = {4, 13, 22, 31, 40, 49};
        char[] expected = {'U', 'R', 'F', 'D', 'L', 'B'};
        for (int i = 0; i < centerIndices.length; i++) {
            assertEquals(expected[i], facelets.charAt(centerIndices[i]));
        }
    }
}
