package com.dctimer.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmartCubeResultTest {
    private static final String SOLVED = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";

    @Test
    public void ganResultUsesDeviceMoveDeltasWithoutScaling() {
        SmartCube cube = new SmartCube();
        cube.setType(BLEDevice.TYPE_GANI_CUBE);
        cube.setCubeState(SOLVED);

        cube.applyMove(0, 0, null);
        cube.markSolveStarted(SOLVED);
        cube.applyMove(3, 500, null);
        cube.applyMove(6, 700, null);

        cube.calcResult();

        assertEquals(1200, cube.getResult());
    }
}
