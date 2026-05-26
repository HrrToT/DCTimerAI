package com.dctimer.model;

import android.util.Log;

import com.dctimer.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cs.min2phase.CubieCube;
import cs.min2phase.Util;

public class SmartCube implements Serializable {
    private int type;
    private int version;
    private String deviceName;
    private String cubeState;
    private int batteryValue;
    private List<Integer> rawData;
    private CubieCube cc;
    private int preIdx;
    private String solveStartState;
    private transient StateChangedCallback callback;
    private String targetState;
    private boolean scrambledNotified;
    private SmartCubeOrientation orientation;
    private transient OrientationChangedCallback orientationChangedCallback;
    private int stateGeneration = 0;
    // 最后一次 solve 的结果，供 UI 显示用（updateTime() 等）
    // 真正的存档数据在 SolveSnapshot 中
    private int lastResult;

    public SmartCube() {
        rawData = new ArrayList<>();
        cc = new CubieCube();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getCubeState() {
        return cubeState;
    }

    public int setCubeState(String state) {
        this.cubeState = state;
        return Util.toCubieCube(state, cc);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getBatteryValue() {
        return batteryValue;
    }

    public void setBatteryValue(int batteryValue) {
        this.batteryValue = batteryValue;
    }

    public void setStateChangedCallback(StateChangedCallback callback) {
        this.callback = callback;
    }

    public void setOrientationChangedCallback(OrientationChangedCallback callback) {
        this.orientationChangedCallback = callback;
    }

    public SmartCubeOrientation getOrientation() {
        return orientation;
    }

    public void setOrientation(SmartCubeOrientation orientation) {
        this.orientation = orientation;
        if (orientationChangedCallback != null) {
            orientationChangedCallback.onOrientationChanged(this, orientation);
        }
    }

    public int getResult() {
        return lastResult;
    }

    public int getStateGeneration() {
        return stateGeneration;
    }

    public void applyMove(int move, int time, String scramble) {
        rawData.add(move << 16 | time);
        cc = cc.move(move);
        cubeState = Util.toFaceCube(cc);
        if (scramble != null && !scramble.equals(targetState)) {
            targetState = scramble;
            scrambledNotified = false;
        }
        if (!scrambledNotified && callback != null && Utils.isSameStateIgnoringRotation(cubeState, scramble)) {
            scrambledNotified = true;
            callback.onScrambled(this);
        }
        if (callback != null && Utils.isSolvedIgnoringRotation(cubeState))
            callback.onSolved(this);
        else
            stateGeneration++;
    }

    public void markScrambled() {
        preIdx = rawData.size();
        solveStartState = cubeState;
        scrambledNotified = true;
    }

    public void markSolveStarted(String startState) {
        if (rawData.isEmpty()) {
            preIdx = 0;
        } else {
            preIdx = rawData.size() - 1;
        }
        solveStartState = startState;
    }

    public void markSolved() {
        stateGeneration++;
        cubeState = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
        cc = new CubieCube();
        rawData = new ArrayList<>();
        preIdx = 0;
        solveStartState = null;
        targetState = null;
        scrambledNotified = false;
        lastResult = 0;
    }

    public SolveSnapshot freezeSnapshot() {
        final int startIdx = this.preIdx;
        List<SmartCubeSolveReconstruction.MoveEvent> solveMoves = new ArrayList<>();
        int elapsed = 0;
        int result = 0;
        for (int i = startIdx; i < rawData.size(); i++) {
            int data = rawData.get(i);
            int delta = data & 0xffff;
            if (i != startIdx) {
                result += delta;
                elapsed += delta;
            }
            int move = data >> 16;
            solveMoves.add(new SmartCubeSolveReconstruction.MoveEvent(move, delta, elapsed));
        }
        SmartCubeSolveReconstruction reconstruction = SmartCubeSolveReconstruction.fromRawMoves(solveStartState, solveMoves);
        lastResult = result;
        return new SolveSnapshot(result, reconstruction);
    }

    public interface StateChangedCallback {
        void onScrambled(SmartCube cube);
        void onSolved(SmartCube cube);
    }

    public interface OrientationChangedCallback {
        void onOrientationChanged(SmartCube cube, SmartCubeOrientation orientation);
    }
}
