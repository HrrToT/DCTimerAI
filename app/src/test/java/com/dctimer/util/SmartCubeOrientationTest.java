package com.dctimer.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmartCubeOrientationTest {
    private static final String SOLVED = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";

    @Test
    public void defaultOrientationKeepsFaceletStateAndMovesUnchanged() {
        assertEquals(SOLVED, Utils.orientFacelets(SOLVED, 0));
        for (int move = 0; move < 18; move++) {
            assertEquals(move, Utils.orientSmartCubeMove(move, 0));
        }
    }
}
