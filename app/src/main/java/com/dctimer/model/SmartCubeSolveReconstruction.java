package com.dctimer.model;

import com.dctimer.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.dctimer.APP.smartCubeSolveOrientation;

import cs.min2phase.CubieCube;
import cs.min2phase.Util;

public class SmartCubeSolveReconstruction {
    private static final String SOLVED_FACELET = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
    private static final String PRETTY_FACES = "URFDLBEMS";
    private static final String SUFFIXES = " 2'";
    private static final String[] PHASE_NAMES = {"Cross", "F2L 1", "F2L 2", "F2L 3", "F2L 4", "OLL", "PLL"};
    private static final int[][] SLICE_FACE_PAIRS = {
            {0, 3}, // E: U + D'
            {4, 1}, // M: L + R'
            {2, 5}  // S: F + B'
    };

    private static final int[][] CROSS_MASK = toEqus("----U--------R--R-----F--F--D-DDD-D-----L--L-----B--B-");
    private static final int[][] F2L1_MASK = toEqus("----U-------RR-RR-----FF-FF-DDDDD-D-----L--L-----B--B-");
    private static final int[][] F2L2_MASK = toEqus("----U--------R--R----FF-FF-DD-DDD-D-----LL-LL----B--B-");
    private static final int[][] F2L3_MASK = toEqus("----U--------RR-RR----F--F--D-DDD-DD----L--L----BB-BB-");
    private static final int[][] F2L4_MASK = toEqus("----U--------R--R-----F--F--D-DDDDD----LL-LL-----BB-BB");
    private static final int[][] F2L_MASK = toEqus("----U-------RRRRRR---FFFFFFDDDDDDDDD---LLLLLL---BBBBBB");
    private static final int[][] OLL_MASK = toEqus("UUUUUUUUU---RRRRRR---FFFFFFDDDDDDDDD---LLLLLL---BBBBBB");
    private static final int[][] SOLVED_MASK = toEqus(SOLVED_FACELET);

    private final List<MoveEvent> rawMoves;
    private final List<PrettyMove> physicalMoves;
    private final List<PrettyMove> displayMoves;
    private final List<ReplayStep> replaySteps;
    private final List<Phase> phases;
    private final String prettySolve;
    private final String moveSequence;
    private final String physicalMoveSequence;
    private final int moveCount;

    private SmartCubeSolveReconstruction(List<MoveEvent> rawMoves, List<PrettyMove> physicalMoves, List<ReplayStep> replaySteps, List<Phase> phases) {
        this.rawMoves = rawMoves;
        this.physicalMoves = physicalMoves;
        this.replaySteps = replaySteps;
        this.displayMoves = extractDisplayMoves(replaySteps);
        this.phases = phases;
        this.prettySolve = buildPrettySolve(phases, this.displayMoves);
        this.moveSequence = buildMoveSequence(this.displayMoves);
        this.physicalMoveSequence = buildMoveSequence(physicalMoves);
        this.moveCount = countNonRotationMoves(this.displayMoves);
    }

    public static SmartCubeSolveReconstruction fromRawMoves(String startFacelet, List<MoveEvent> rawMoves) {
        List<MoveEvent> safeRawMoves = copyRawMoves(rawMoves);
        List<MoveEvent> displayRawMoves = orientRawMoves(safeRawMoves, smartCubeSolveOrientation);
        int[] phaseByRawIndex = buildPhaseByRawIndex(startFacelet, safeRawMoves);
        List<PrettyMove> physicalMoves = reconstructPhysicalMoves(displayRawMoves, phaseByRawIndex);
        List<ReplayStep> replaySteps = buildReplaySteps(physicalMoves);
        List<Phase> phases = hasPhaseData(phaseByRawIndex)
                ? buildPhasesFromReplaySteps(replaySteps) : createEmptyPhases();
        return new SmartCubeSolveReconstruction(displayRawMoves, physicalMoves, replaySteps, phases);
    }

    private static List<MoveEvent> copyRawMoves(List<MoveEvent> rawMoves) {
        List<MoveEvent> result = new ArrayList<>();
        if (rawMoves == null) {
            return result;
        }
        result.addAll(rawMoves);
        return result;
    }

    private static List<MoveEvent> orientRawMoves(List<MoveEvent> rawMoves, int orientationIndex) {
        List<MoveEvent> result = new ArrayList<>();
        if (rawMoves == null) {
            return result;
        }
        for (MoveEvent rawMove : rawMoves) {
            result.add(new MoveEvent(Utils.orientSmartCubeMove(rawMove.move, orientationIndex), rawMove.deltaMs, rawMove.elapsedMs));
        }
        return result;
    }

    public String getPrettySolve() {
        return prettySolve;
    }

    public String getPrettySolve(int solveTimeMs) {
        return appendSolveStats(prettySolve, solveTimeMs);
    }

    public String getMoveSequence() {
        return moveSequence;
    }

    public String getPhysicalMoveSequence() {
        return physicalMoveSequence;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public String toJson(int solveTimeMs) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendJsonField(sb, "version", "2");
        sb.append(',');
        appendJsonField(sb, "method", "333-smart-cf4op");
        sb.append(',');
        sb.append("\"moveCount\":").append(moveCount);
        sb.append(',');
        sb.append("\"solveTimeMs\":").append(Math.max(0, solveTimeMs));
        sb.append(',');
        sb.append("\"moveDeltas\":[");
        for (int i = 0; i < rawMoves.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(rawMoves.get(i).deltaMs);
        }
        sb.append(']');
        sb.append(',');
        appendJsonField(sb, "moves", moveSequence);
        sb.append(',');
        appendJsonField(sb, "physicalMoves", physicalMoveSequence);
        sb.append(',');
        appendJsonField(sb, "displayMoves", moveSequence);
        sb.append(',');
        appendDisplayStepsJson(sb);
        sb.append(',');
        appendJsonField(sb, "prettySolve", getPrettySolve(solveTimeMs));
        sb.append(',');
        sb.append("\"phases\":[");
        for (int i = 0; i < phases.size(); i++) {
            if (i > 0) sb.append(',');
            Phase phase = phases.get(i);
            sb.append('{');
            appendJsonField(sb, "name", phase.name);
            sb.append(',');
            sb.append("\"moveCount\":").append(phase.moveCount);
            sb.append(',');
            sb.append("\"physicalMoveCount\":").append(phase.physicalMoveCount);
            sb.append(',');
            sb.append("\"displayStart\":").append(phase.displayStart);
            sb.append(',');
            sb.append("\"displayEnd\":").append(phase.displayEnd);
            sb.append(',');
            sb.append("\"physicalStart\":").append(phase.physicalStart);
            sb.append(',');
            sb.append("\"physicalEnd\":").append(phase.physicalEnd);
            sb.append(',');
            sb.append("\"startMs\":").append(phase.startMs);
            sb.append(',');
            sb.append("\"endMs\":").append(phase.endMs);
            sb.append(',');
            appendJsonField(sb, "moves", phase.moves);
            sb.append('}');
        }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    public static class MoveEvent {
        public final int move;
        public final int deltaMs;
        public final int elapsedMs;

        public MoveEvent(int move, int deltaMs, int elapsedMs) {
            this.move = move;
            this.deltaMs = deltaMs;
            this.elapsedMs = elapsedMs;
        }
    }

    private static class PrettyMove {
        final int axis;
        int pow;
        int startRawIndex;
        int endRawIndex;
        int startMs;
        int endMs;
        int phaseIndex;

        PrettyMove(int axis, int pow, int startRawIndex, int endRawIndex, int startMs, int endMs) {
            this(axis, pow, startRawIndex, endRawIndex, startMs, endMs, -1);
        }

        PrettyMove(int axis, int pow, int startRawIndex, int endRawIndex, int startMs, int endMs, int phaseIndex) {
            this.axis = axis;
            this.pow = pow;
            this.startRawIndex = startRawIndex;
            this.endRawIndex = endRawIndex;
            this.startMs = startMs;
            this.endMs = endMs;
            this.phaseIndex = phaseIndex;
        }

        String notation() {
            return String.valueOf(PRETTY_FACES.charAt(axis)) + SUFFIXES.charAt(pow);
        }

        PrettyMove copy() {
            return new PrettyMove(axis, pow, startRawIndex, endRawIndex, startMs, endMs, phaseIndex);
        }

        boolean isRotation() {
            return false;
        }
    }

    private static class Phase {
        final String name;
        final String moves;
        final int moveCount;
        final int physicalMoveCount;
        final int displayStart;
        final int displayEnd;
        final int physicalStart;
        final int physicalEnd;
        final int startMs;
        final int endMs;

        Phase(String name, String moves, int moveCount, int startMs, int endMs) {
            this(name, moves, moveCount, 0, -1, -1, -1, -1, startMs, endMs);
        }

        Phase(String name, String moves, int moveCount, int physicalMoveCount,
              int displayStart, int displayEnd, int physicalStart, int physicalEnd,
              int startMs, int endMs) {
            this.name = name;
            this.moves = moves;
            this.moveCount = moveCount;
            this.physicalMoveCount = physicalMoveCount;
            this.displayStart = displayStart;
            this.displayEnd = displayEnd;
            this.physicalStart = physicalStart;
            this.physicalEnd = physicalEnd;
            this.startMs = startMs;
            this.endMs = endMs;
        }
    }

    private static class ReplayStep {
        final PrettyMove displayMove;
        final List<PrettyMove> physicalMoves;

        ReplayStep(PrettyMove displayMove, List<PrettyMove> physicalMoves) {
            this.displayMove = displayMove;
            this.physicalMoves = physicalMoves;
        }
    }

    private static class PhaseBuildResult {
        final List<PrettyMove> reconstructedMoves;
        final List<Phase> phases;

        PhaseBuildResult(List<PrettyMove> reconstructedMoves, List<Phase> phases) {
            this.reconstructedMoves = reconstructedMoves;
            this.phases = phases;
        }
    }

    private static int[] buildPhaseByRawIndex(String startFacelet, List<MoveEvent> rawMoves) {
        int[] phaseByRawIndex = new int[rawMoves.size()];
        for (int i = 0; i < phaseByRawIndex.length; i++) {
            phaseByRawIndex[i] = -1;
        }
        if (rawMoves.isEmpty() || isEmpty(startFacelet)) {
            return phaseByRawIndex;
        }
        CubieCube cube = new CubieCube();
        if (Util.toCubieCube(startFacelet, cube) != 0) {
            return phaseByRawIndex;
        }

        int status = updatePhaseStatus(PHASE_NAMES.length, getCf4opProgress(startFacelet));
        for (int i = 0; i < rawMoves.size(); i++) {
            phaseByRawIndex[i] = PHASE_NAMES.length - status;
            int move = rawMoves.get(i).move;
            if (move >= 0 && move < 18) {
                cube = cube.move(move);
            }
            status = updatePhaseStatus(status, getCf4opProgress(Util.toFaceCube(cube)));
        }
        return phaseByRawIndex;
    }

    private static boolean hasPhaseData(int[] phaseByRawIndex) {
        for (int phaseIndex : phaseByRawIndex) {
            if (phaseIndex >= 0) {
                return true;
            }
        }
        return false;
    }

    private static List<PrettyMove> reconstructPhysicalMoves(List<MoveEvent> rawMoves) {
        return reconstructPhysicalMoves(rawMoves, null);
    }

    private static List<PrettyMove> reconstructPhysicalMoves(List<MoveEvent> rawMoves, int[] phaseByRawIndex) {
        List<PrettyMove> ret = new ArrayList<>();
        for (int i = 0; i < rawMoves.size(); i++) {
            MoveEvent current = rawMoves.get(i);
            int axis = current.move / 3;
            int pow = current.move % 3;
            if (axis < 0 || axis >= PRETTY_FACES.length()) {
                continue;
            }
            int phaseIndex = phaseByRawIndex != null && i < phaseByRawIndex.length ? phaseByRawIndex[i] : -1;
            pushMove(ret, axis, pow, i, i, current.elapsedMs, current.elapsedMs, phaseIndex);
        }
        return ret;
    }

    private static List<PrettyMove> compressDisplayMoves(List<PrettyMove> physicalMoves) {
        return extractDisplayMoves(buildReplaySteps(physicalMoves));
    }

    private static List<ReplayStep> buildReplaySteps(List<PrettyMove> physicalMoves) {
        List<ReplayStep> replaySteps = new ArrayList<>();
        for (int i = 0; i < physicalMoves.size(); i++) {
            PrettyMove current = physicalMoves.get(i);
            if (i < physicalMoves.size() - 1) {
                PrettyMove next = physicalMoves.get(i + 1);
                PrettyMove sliceMove = current.phaseIndex == next.phaseIndex
                        ? tryBuildSliceMove(current, next) : null;
                if (sliceMove != null) {
                    List<PrettyMove> physicalGroup = new ArrayList<>();
                    physicalGroup.add(current.copy());
                    physicalGroup.add(next.copy());
                    replaySteps.add(new ReplayStep(sliceMove, physicalGroup));
                    i++;
                    continue;
                }
            }
            List<PrettyMove> physicalGroup = new ArrayList<>();
            physicalGroup.add(current.copy());
            replaySteps.add(new ReplayStep(current.copy(), physicalGroup));
        }
        return replaySteps;
    }

    private static List<PrettyMove> extractDisplayMoves(List<ReplayStep> replaySteps) {
        List<PrettyMove> result = new ArrayList<>();
        for (ReplayStep step : replaySteps) {
            result.add(step.displayMove.copy());
        }
        return result;
    }

    private static PrettyMove tryBuildSliceMove(PrettyMove first, PrettyMove second) {
        for (int sliceAxis = 0; sliceAxis < SLICE_FACE_PAIRS.length; sliceAxis++) {
            int primaryFace = SLICE_FACE_PAIRS[sliceAxis][0];
            int oppositeFace = SLICE_FACE_PAIRS[sliceAxis][1];
            int slicePow = matchSlicePow(first, second, primaryFace, oppositeFace);
            if (slicePow >= 0) {
                return new PrettyMove(sliceAxis + 6, slicePow, first.startRawIndex, second.endRawIndex,
                        first.startMs, second.endMs, first.phaseIndex);
            }
        }
        return null;
    }

    private static int matchSlicePow(PrettyMove first, PrettyMove second, int primaryFace, int oppositeFace) {
        if (isMovePair(first, second, primaryFace, 0, oppositeFace, 2)
                || isMovePair(first, second, oppositeFace, 2, primaryFace, 0)) {
            return 0;
        }
        if (isMovePair(first, second, primaryFace, 1, oppositeFace, 1)
                || isMovePair(first, second, oppositeFace, 1, primaryFace, 1)) {
            return 1;
        }
        if (isMovePair(first, second, primaryFace, 2, oppositeFace, 0)
                || isMovePair(first, second, oppositeFace, 0, primaryFace, 2)) {
            return 2;
        }
        return -1;
    }

    private static boolean isMovePair(PrettyMove first, PrettyMove second, int face1, int pow1, int face2, int pow2) {
        return first.axis == face1 && first.pow == pow1
                && second.axis == face2 && second.pow == pow2;
    }

    private static void pushMove(List<PrettyMove> moves, int axis, int pow, int startRawIndex, int endRawIndex, int startMs, int endMs, int phaseIndex) {
        if (moves.isEmpty() || moves.get(moves.size() - 1).axis != axis
                || moves.get(moves.size() - 1).phaseIndex != phaseIndex) {
            moves.add(new PrettyMove(axis, pow, startRawIndex, endRawIndex, startMs, endMs, phaseIndex));
            return;
        }
        PrettyMove last = moves.get(moves.size() - 1);
        int mergedPow = (pow + last.pow + 1) % 4;
        if (mergedPow == 3) {
            moves.remove(moves.size() - 1);
        } else {
            last.pow = mergedPow;
            last.endRawIndex = endRawIndex;
            last.endMs = endMs;
        }
    }

    private static PhaseBuildResult buildPhases(String startFacelet, List<MoveEvent> rawMoves, List<MoveEvent> displayRawMoves) {
        if (rawMoves.isEmpty() || isEmpty(startFacelet)) {
            List<PrettyMove> displayMoves = compressDisplayMoves(reconstructPhysicalMoves(displayRawMoves));
            return new PhaseBuildResult(displayMoves, createEmptyPhases());
        }
        CubieCube cube = new CubieCube();
        if (Util.toCubieCube(startFacelet, cube) != 0) {
            List<PrettyMove> displayMoves = compressDisplayMoves(reconstructPhysicalMoves(displayRawMoves));
            return new PhaseBuildResult(displayMoves, createEmptyPhases());
        }

        List<List<MoveEvent>> statusBuckets = new ArrayList<>();
        for (int i = 0; i < PHASE_NAMES.length; i++) {
            statusBuckets.add(new ArrayList<MoveEvent>());
        }

        int status = updatePhaseStatus(PHASE_NAMES.length, getCf4opProgress(startFacelet));
        for (int i = 0; i < rawMoves.size(); i++) {
            MoveEvent rawMove = rawMoves.get(i);
            statusBuckets.get(status - 1).add(displayRawMoves.get(i));
            int move = rawMove.move;
            if (move >= 0 && move < 18) {
                cube = cube.move(move);
            }
            status = updatePhaseStatus(status, getCf4opProgress(Util.toFaceCube(cube)));
        }

        List<List<MoveEvent>> phaseRawMoves = new ArrayList<>();
        List<String> phaseNames = new ArrayList<>();
        for (int i = PHASE_NAMES.length - 1; i >= 0; i--) {
            phaseRawMoves.add(statusBuckets.get(i));
            phaseNames.add(PHASE_NAMES[PHASE_NAMES.length - 1 - i]);
        }
        List<List<PrettyMove>> phasePrettyMoves = reconstructDisplayMoveGroups(phaseRawMoves);
        List<Phase> phases = new ArrayList<>();
        List<PrettyMove> reconstructedMoves = new ArrayList<>();
        for (int i = 0; i < phaseNames.size(); i++) {
            List<PrettyMove> moves = phasePrettyMoves.get(i);
            reconstructedMoves.addAll(moves);
            phases.add(createPhase(phaseNames.get(i), moves, 0, moves.size() - 1));
        }
        return new PhaseBuildResult(reconstructedMoves, phases);
    }

    private static List<Phase> buildPhasesFromReplaySteps(List<ReplayStep> replaySteps) {
        List<Phase> phases = new ArrayList<>();
        for (int phaseIndex = 0; phaseIndex < PHASE_NAMES.length; phaseIndex++) {
            int displayStart = -1;
            int displayEnd = -1;
            int physicalStart = -1;
            int physicalEnd = -1;
            int physicalMoveCount = 0;
            StringBuilder moves = new StringBuilder();
            int startMs = 0;
            int endMs = 0;

            for (int stepIndex = 0; stepIndex < replaySteps.size(); stepIndex++) {
                ReplayStep step = replaySteps.get(stepIndex);
                if (step.displayMove.phaseIndex != phaseIndex) {
                    continue;
                }
                if (displayStart < 0) {
                    displayStart = stepIndex;
                    physicalStart = step.displayMove.startRawIndex;
                    startMs = step.displayMove.startMs;
                }
                displayEnd = stepIndex;
                physicalEnd = step.displayMove.endRawIndex;
                endMs = step.displayMove.endMs;
                physicalMoveCount += step.physicalMoves.size();
                if (moves.length() > 0) {
                    moves.append(' ');
                }
                moves.append(step.displayMove.notation().trim());
            }

            if (displayStart < 0) {
                phases.add(new Phase(PHASE_NAMES[phaseIndex], "", 0, 0,
                        -1, -1, -1, -1, 0, 0));
            } else {
                phases.add(new Phase(PHASE_NAMES[phaseIndex], moves.toString(),
                        displayEnd - displayStart + 1, physicalMoveCount,
                        displayStart, displayEnd, physicalStart, physicalEnd,
                        startMs, endMs));
            }
        }
        return phases;
    }

    private static int updatePhaseStatus(int status, int progress) {
        int nextStatus = Math.min(progress, status);
        return nextStatus == 0 ? 1 : nextStatus;
    }

    private static List<Phase> createEmptyPhases() {
        List<Phase> phases = new ArrayList<>();
        for (String phaseName : PHASE_NAMES) {
            phases.add(new Phase(phaseName, "", 0, 0, 0));
        }
        return phases;
    }

    private static List<List<PrettyMove>> reconstructDisplayMoveGroups(List<List<MoveEvent>> rawMoveGroups) {
        List<List<PrettyMove>> result = new ArrayList<>();
        for (List<MoveEvent> rawMoveGroup : rawMoveGroups) {
            result.add(compressDisplayMoves(reconstructPhysicalMoves(rawMoveGroup)));
        }
        return result;
    }

    private static Phase createPhase(String name, List<PrettyMove> moves, int start, int end) {
        if (moves.isEmpty() || start >= moves.size() || end < start) {
            return new Phase(name, "", 0, 0, 0);
        }
        int safeEnd = Math.min(end, moves.size() - 1);
        String moveText = joinMoves(moves, start, safeEnd);
        int count = countNonRotationMoves(moves, start, safeEnd);
        return new Phase(name, moveText, count, moves.get(start).startMs, moves.get(safeEnd).endMs);
    }

    private static int getCf4opProgress(String facelet) {
        List<String> variants = Utils.getAxisOrientationVariants(facelet);
        if (variants.isEmpty()) {
            variants.add(facelet);
        }
        int minProgress = 99;
        for (String variant : variants) {
            int progress = getCf4opProgressForAxis(variant);
            if (progress < minProgress) {
                minProgress = progress;
            }
        }
        return minProgress == 99 ? 7 : minProgress;
    }

    private static int getCf4opProgressForAxis(String facelet) {
        if (isUnsolvedForMask(facelet, CROSS_MASK)) {
            return 7;
        } else if (isUnsolvedForMask(facelet, F2L_MASK)) {
            return 2
                    + (isUnsolvedForMask(facelet, F2L1_MASK) ? 1 : 0)
                    + (isUnsolvedForMask(facelet, F2L2_MASK) ? 1 : 0)
                    + (isUnsolvedForMask(facelet, F2L3_MASK) ? 1 : 0)
                    + (isUnsolvedForMask(facelet, F2L4_MASK) ? 1 : 0);
        } else if (isUnsolvedForMask(facelet, OLL_MASK)) {
            return 2;
        } else if (isUnsolvedForMask(facelet, SOLVED_MASK)) {
            return 1;
        }
        return 0;
    }

    private static boolean isUnsolvedForMask(String facelet, int[][] mask) {
        return !isSolvedForMask(facelet, mask);
    }

    private static boolean isSolvedForMask(String facelet, int[][] mask) {
        if (isEmpty(facelet) || facelet.length() < 54) {
            return false;
        }
        for (int[] equ : mask) {
            if (equ.length == 0) {
                continue;
            }
            char color = facelet.charAt(equ[0]);
            for (int i = 1; i < equ.length; i++) {
                if (facelet.charAt(equ[i]) != color) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int[][] toEqus(String facelet) {
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < facelet.length(); i++) {
            char color = facelet.charAt(i);
            if (color == '-') {
                continue;
            }
            List<Integer> indices = new ArrayList<>();
            for (int j = i; j < facelet.length(); j++) {
                if (facelet.charAt(j) == color) {
                    indices.add(j);
                }
            }
            if (indices.size() > 1) {
                int[] equ = new int[indices.size()];
                for (int j = 0; j < indices.size(); j++) {
                    equ[j] = indices.get(j);
                }
                result.add(equ);
            }
            facelet = facelet.replace(color, '-');
        }
        return result.toArray(new int[result.size()][]);
    }

    private static String buildMoveSequence(List<PrettyMove> moves) {
        return joinMoves(moves, 0, moves.size() - 1);
    }

    private static String buildPrettySolve(List<Phase> phases, List<PrettyMove> moves) {
        if (phases.isEmpty()) {
            return buildMoveSequence(moves);
        }
        StringBuilder sb = new StringBuilder();
        for (Phase phase : phases) {
            if (isEmpty(phase.moves)) {
                continue;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(phase.moves)
                    .append(" // ")
                    .append(phase.name);
        }
        if (sb.length() == 0) {
            return buildMoveSequence(moves);
        }
        return sb.toString();
    }

    private String appendSolveStats(String solveText, int solveTimeMs) {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(solveText)) {
            sb.append(solveText.trim());
            sb.append('\n');
        }
        sb.append("Total: ")
                .append(moveCount)
                .append(" moves");
        sb.append('\n');
        sb.append("TPS: ")
                .append(String.format(Locale.US, "%.1f", solveTimeMs > 0 ? moveCount * 1000f / solveTimeMs : 0f))
                .append(" tps");
        return sb.toString();
    }

    private static String joinMoves(List<PrettyMove> moves, int start, int end) {
        if (moves == null || moves.isEmpty() || end < start) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int safeStart = Math.max(0, start);
        int safeEnd = Math.min(end, moves.size() - 1);
        for (int i = safeStart; i <= safeEnd; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(moves.get(i).notation().trim());
        }
        return sb.toString();
    }

    private static int countNonRotationMoves(List<PrettyMove> moves) {
        return countNonRotationMoves(moves, 0, moves.size() - 1);
    }

    private static int countNonRotationMoves(List<PrettyMove> moves, int start, int end) {
        if (moves == null || moves.isEmpty() || end < start) {
            return 0;
        }
        int count = 0;
        int safeEnd = Math.min(end, moves.size() - 1);
        for (int i = Math.max(0, start); i <= safeEnd; i++) {
            if (!moves.get(i).isRotation()) {
                count++;
            }
        }
        return count;
    }

    private void appendDisplayStepsJson(StringBuilder sb) {
        sb.append("\"displaySteps\":[");
        for (int i = 0; i < replaySteps.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            ReplayStep step = replaySteps.get(i);
            PrettyMove displayMove = step.displayMove;
            sb.append('{');
            sb.append("\"index\":").append(i);
            sb.append(',');
            appendJsonField(sb, "notation", displayMove.notation().trim());
            sb.append(',');
            sb.append("\"phaseIndex\":").append(displayMove.phaseIndex);
            sb.append(',');
            appendJsonField(sb, "phaseName", phaseName(displayMove.phaseIndex));
            sb.append(',');
            sb.append("\"startMs\":").append(displayMove.startMs);
            sb.append(',');
            sb.append("\"endMs\":").append(displayMove.endMs);
            sb.append(',');
            sb.append("\"deltaMs\":").append(sumRawDeltas(displayMove.startRawIndex, displayMove.endRawIndex));
            sb.append(',');
            sb.append("\"physicalStart\":").append(displayMove.startRawIndex);
            sb.append(',');
            sb.append("\"physicalEnd\":").append(displayMove.endRawIndex);
            sb.append(',');
            appendPhysicalMovesJson(sb, step.physicalMoves);
            sb.append(',');
            appendPhysicalDeltasJson(sb, step.physicalMoves);
            sb.append('}');
        }
        sb.append(']');
    }

    private void appendPhysicalMovesJson(StringBuilder sb, List<PrettyMove> physicalMoves) {
        sb.append("\"physicalMoves\":[");
        for (int i = 0; i < physicalMoves.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendJsonString(sb, physicalMoves.get(i).notation().trim());
        }
        sb.append(']');
    }

    private void appendPhysicalDeltasJson(StringBuilder sb, List<PrettyMove> physicalMoves) {
        sb.append("\"physicalDeltas\":[");
        for (int i = 0; i < physicalMoves.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            PrettyMove move = physicalMoves.get(i);
            sb.append(sumRawDeltas(move.startRawIndex, move.endRawIndex));
        }
        sb.append(']');
    }

    private int sumRawDeltas(int startRawIndex, int endRawIndex) {
        if (rawMoves == null || rawMoves.isEmpty() || endRawIndex < startRawIndex) {
            return 0;
        }
        int safeStart = Math.max(0, startRawIndex);
        int safeEnd = Math.min(endRawIndex, rawMoves.size() - 1);
        int sum = 0;
        for (int i = safeStart; i <= safeEnd; i++) {
            sum += Math.max(0, rawMoves.get(i).deltaMs);
        }
        return sum;
    }

    private static String phaseName(int phaseIndex) {
        if (phaseIndex < 0 || phaseIndex >= PHASE_NAMES.length) {
            return "";
        }
        return PHASE_NAMES[phaseIndex];
    }

    private static void appendJsonField(StringBuilder sb, String key, String value) {
        appendJsonString(sb, key);
        sb.append(':');
        appendJsonString(sb, value);
    }

    private static void appendJsonString(StringBuilder sb, String value) {
        sb.append('"').append(escapeJson(value)).append('"');
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format(Locale.US, "\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
