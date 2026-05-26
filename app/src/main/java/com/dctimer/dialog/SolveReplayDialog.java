package com.dctimer.dialog;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dctimer.APP;
import com.dctimer.R;
import com.dctimer.util.SliceMoveUtils;
import com.dctimer.util.Utils;
import com.dctimer.view.SmartCube3DView;
import com.dctimer.view.SolveReplayRenderer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cs.min2phase.CubieCube;

public class SolveReplayDialog extends DialogFragment {
    private static final String SOLVED = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
    private static final String[] FACE_CHARS = {"U", "R", "F", "D", "L", "B"};
    private static final String[] SUFFIX = {"", "2", "'"};

    private SmartCube3DView cube3DView;
    private SolveReplayRenderer sparklineView;
    private TextView tvMoveInfo, tvStats;
    private Button btnPlay, btnSpeed;

    private List<SolveReplayRenderer.MoveStep> moveSteps = new ArrayList<>();
    private List<String> faceletStates = new ArrayList<>();
    private List<List<String>> stepFaceletStates = new ArrayList<>();
    private List<SolveReplayRenderer.PhaseInfo> phaseInfos = new ArrayList<>();
    private int totalTimeMs;
    private int inspectionMs;
    private int currentIndex = 0;
    private int animatingStepIndex = -1;
    private int animatingSubMoveIndex = -1;
    private boolean isPlaying = false;
    private float speed = 1.0f;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static SolveReplayDialog newInstance(String scramble, String moves, String solveMeta) {
        SolveReplayDialog dialog = new SolveReplayDialog();
        Bundle args = new Bundle();
        args.putString("scramble", scramble);
        args.putString("moves", moves);
        args.putString("solve_meta", solveMeta);
        dialog.setArguments(args);
        return dialog;
    }

    private void logError(String tag, Throwable e) {
        try {
            java.io.File file = new java.io.File(getActivity().getFilesDir(), "replay_crash.txt");
            java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(file, true));
            pw.println("=== " + tag + " ===");
            pw.println("Time: " + System.currentTimeMillis());
            if (e != null) e.printStackTrace(pw);
            pw.close();
        } catch (Exception ignored) {}
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        try {
            return doCreateDialog(savedInstanceState);
        } catch (Throwable e) {
            logError("onCreateDialog", e);
            throw e;
        }
    }

    private Dialog doCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_solve_replay, null);
        applyReplayBackground(view);

        tvMoveInfo = view.findViewById(R.id.tv_move_info);
        sparklineView = view.findViewById(R.id.sparkline);
        tvStats = view.findViewById(R.id.tv_stats);
        applyReplayColors(view.findViewById(R.id.v_info_divider));

        FrameLayout flCube = view.findViewById(R.id.fl_cube);
        cube3DView = new SmartCube3DView(getActivity());
        cube3DView.setReplayInteractionEnabled(true);
        cube3DView.resetOrientationToWhiteTopGreenFront();
        cube3DView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        flCube.addView(cube3DView);

        cube3DView.setOnAnimationEndListener(new SmartCube3DView.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleAnimationEnd();
                    }
                });
            }
        });

        sparklineView.setOnStepClickListener(new SolveReplayRenderer.OnStepClickListener() {
            @Override
            public void onStepClick(int step) {
                jumpToStep(step);
            }
        });

        btnPlay = view.findViewById(R.id.btn_play);
        Button btnStart = view.findViewById(R.id.btn_start);
        Button btnPrev = view.findViewById(R.id.btn_prev);
        Button btnNext = view.findViewById(R.id.btn_next);
        Button btnEnd = view.findViewById(R.id.btn_end);
        btnSpeed = view.findViewById(R.id.btn_speed);
        Button btnClose = view.findViewById(R.id.btn_close);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { jumpToStep(0); }
        });
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { stepPrev(); }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { togglePlay(); }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { stepNext(); }
        });
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { jumpToStep(moveSteps.size()); }
        });
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { cycleSpeed(); }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dismiss(); }
        });

        parseData();

        runEmsDebug();

        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void runEmsDebug() {
        try {
            java.io.File file = new java.io.File(getActivity().getFilesDir(), "debug_ems.txt");
            java.io.PrintWriter pw = new java.io.PrintWriter(file);
            pw.println("=== EMS Debug ===");
            String[] tests = {"E", "E'", "M", "M'", "S", "S'"};
            String[] labels = {"E (CW)", "E' (CCW)", "M (CW)", "M' (CCW)", "S (CW)", "S' (CCW)"};
            for (int t = 0; t < tests.length; t++) {
                CubieCube cc = new CubieCube();
                int move = notationToMove(tests[t]);
                pw.println("--- " + labels[t] + " move=" + move + " ---");
                cc = applyMoveToCube(cc, move);
                String fl = cs.min2phase.Util.toFaceCube(cc);
                pw.println("facelet: " + fl);
                pw.println();
            }
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        try {
            super.onStart();
            Dialog dialog = getDialog();
            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            }
            if (getActivity() != null) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } catch (Throwable e) {
            logError("onStart", e);
            throw e;
        }
    }

    @Override
    public void onStop() {
        try {
            super.onStop();
            if (getActivity() != null) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } catch (Throwable e) {
            logError("onStop", e);
            throw e;
        }
    }

    @Override
    public void onResume() {
        try {
            super.onResume();
            if (cube3DView != null) {
                cube3DView.onResume();
                if (!faceletStates.isEmpty()) showState(0);
            }
        } catch (Throwable e) {
            logError("onResume", e);
            throw e;
        }
    }

    @Override
    public void onPause() {
        try {
            isPlaying = false;
            if (cube3DView != null) cube3DView.onPause();
            super.onPause();
        } catch (Throwable e) {
            logError("onPause", e);
        }
    }

    private void parseData() {
        Bundle args = getArguments();
        if (args == null) return;

        String scramble = args.getString("scramble", "");
        String movesText = args.getString("moves", "");
        String solveMeta = args.getString("solve_meta", "");

        List<Integer> moveInts = new ArrayList<>();
        List<Integer> moveDeltas = null;
        int solveTimeMs = 0;
        int[] phaseForMove = null;  // per-move phase assignment via pattern matching
        String replayMovesText = movesText;
        boolean parsedDisplaySteps = false;

        if (!TextUtils.isEmpty(solveMeta)) {
            try {
                JSONObject json = new JSONObject(solveMeta);
                solveTimeMs = json.optInt("solveTimeMs", 0);

                if (json.has("moveDeltas")) {
                    JSONArray arr = json.getJSONArray("moveDeltas");
                    moveDeltas = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        moveDeltas.add(arr.getInt(i));
                    }
                }

                if (json.has("phases")) {
                    JSONArray phaseArr = json.getJSONArray("phases");
                    // Count-based phase assignment: use phase.moveCount directly
                    int totalPhaseMoves = 0;
                    for (int pi = 0; pi < phaseArr.length(); pi++) {
                        JSONObject ph = phaseArr.getJSONObject(pi);
                        int count = ph.optInt("moveCount", 0);
                        String name = ph.optString("name", "");
                        int startMs = ph.optInt("startMs", 0);
                        int endMs = ph.optInt("endMs", 0);
                        int color = getPhaseColor(pi);
                        totalPhaseMoves += count;
                        phaseInfos.add(new SolveReplayRenderer.PhaseInfo(name, startMs, endMs, color, count));
                    }
                }

                if (json.has("displaySteps")) {
                    // v2.2.8+ unified path: displaySteps is the single source of truth
                    parsedDisplaySteps = parseDisplaySteps(json.getJSONArray("displaySteps"));
                } else {
                    // LEGACY: pre-v2.2.8 data without displaySteps, fallback to raw moves text
                    String physicalMoves = json.optString("physicalMoves", "");
                    if (!TextUtils.isEmpty(physicalMoves)) {
                        replayMovesText = physicalMoves;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (parsedDisplaySteps) {
            inspectionMs = moveDeltas != null && moveDeltas.size() > 0 ? moveDeltas.get(0) : 0;
            totalTimeMs = inspectionMs + Math.max(0, solveTimeMs);
            if (totalTimeMs == 0 && !moveSteps.isEmpty()) {
                totalTimeMs = moveSteps.get(moveSteps.size() - 1).cumulativeMs;
            }
            computeFaceletStates(scramble);
            dumpReplayData(scramble, "displaySteps", solveMeta, flattenPhysicalMoves(), null);
            sparklineView.setData(moveSteps, phaseInfos, totalTimeMs, inspectionMs);
            updateUi();
            return;
        }

        // LEGACY: pre-v2.2.8 fallback — re-parse moves text into steps
        moveInts = parseMovesText(replayMovesText);
        if (!phaseInfos.isEmpty()) {
            phaseForMove = buildPhaseForMove(moveInts.size());
        }

        // Time calculation: use solveTimeMs from JSON, add inspection from first raw delta
        inspectionMs = 0;
        if (moveDeltas != null && moveDeltas.size() > 0) {
            inspectionMs = moveDeltas.get(0);
        }
        totalTimeMs = inspectionMs + Math.max(0, solveTimeMs);
        if (totalTimeMs == 0 && moveInts.size() > 0) {
            totalTimeMs = moveInts.size() * 80;  // fallback
        }

        // Build move steps with phase from pattern matching
        float speedPerMove = moveDeltas != null && moveDeltas.size() > 0 && solveTimeMs > 0
                ? (float) solveTimeMs / sumAfterFirst(moveDeltas) : 1f;
        int cumulativeMs = 0;
        for (int i = 0; i < moveInts.size(); i++) {
            int delta;
            if (moveDeltas != null && i < moveDeltas.size()) {
                if (i == 0) {
                    delta = moveDeltas.get(0);  // inspection
                } else {
                    delta = (int) (moveDeltas.get(i) * speedPerMove);
                }
            } else {
                delta = 80;
            }
            cumulativeMs += delta;
            int phaseIdx;
            if (phaseForMove != null && i < phaseForMove.length) {
                phaseIdx = phaseForMove[i];
            } else {
                phaseIdx = findPhaseIndex(cumulativeMs);
            }
            String notation = moveToNotation(moveInts.get(i));
            List<Integer> physicalMoves = new ArrayList<>();
            physicalMoves.add(moveInts.get(i));
            List<Integer> physicalDeltas = new ArrayList<>();
            physicalDeltas.add(delta);
            moveSteps.add(new SolveReplayRenderer.MoveStep(i, notation, delta, cumulativeMs, phaseIdx,
                    physicalMoves, physicalDeltas));
        }

        computeFaceletStates(scramble);

        // Debug: log data to file
        dumpReplayData(scramble, replayMovesText, solveMeta, moveInts, phaseForMove);

        sparklineView.setData(moveSteps, phaseInfos, totalTimeMs, inspectionMs);
        updateUi();
    }

    private boolean parseDisplaySteps(JSONArray steps) {
        moveSteps.clear();
        int cumulativeMs = 0;
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.optJSONObject(i);
            if (step == null) {
                continue;
            }
            String notation = step.optString("notation", "");
            int deltaMs = Math.max(0, step.optInt("deltaMs", 80));
            cumulativeMs += deltaMs;
            int phaseIndex = step.optInt("phaseIndex", findPhaseIndex(cumulativeMs));
            List<Integer> physicalMoves = parsePhysicalMoveArray(step.optJSONArray("physicalMoves"));
            if (physicalMoves.isEmpty()) {
                int move = notationToMove(notation);
                if (move >= 0) {
                    physicalMoves.add(move);
                }
            }
            if (physicalMoves.isEmpty()) {
                continue;
            }
            List<Integer> physicalDeltas = parseIntArray(step.optJSONArray("physicalDeltas"));
            normalizePhysicalDeltas(physicalDeltas, physicalMoves.size(), deltaMs);
            moveSteps.add(new SolveReplayRenderer.MoveStep(moveSteps.size(), notation, deltaMs,
                    cumulativeMs, phaseIndex, physicalMoves, physicalDeltas));
        }
        return !moveSteps.isEmpty();
    }

    private int[] buildPhaseForMove(int moveCount) {
        int[] phaseForMove = new int[moveCount];
        int cumPhaseMoves = 0;
        int curPhase = 0;
        for (int i = 0; i < moveCount; i++) {
            while (curPhase < phaseInfos.size() - 1) {
                SolveReplayRenderer.PhaseInfo pi = phaseInfos.get(curPhase);
                if (pi.moveCount > 0 && i >= cumPhaseMoves + pi.moveCount) {
                    cumPhaseMoves += pi.moveCount;
                    curPhase++;
                } else {
                    break;
                }
            }
            phaseForMove[i] = curPhase;
        }
        return phaseForMove;
    }

    private List<Integer> parsePhysicalMoveArray(JSONArray arr) {
        List<Integer> result = new ArrayList<>();
        if (arr == null) {
            return result;
        }
        for (int i = 0; i < arr.length(); i++) {
            int move = notationToMove(arr.optString(i, ""));
            if (move >= 0) {
                result.add(move);
            }
        }
        return result;
    }

    private List<Integer> parseIntArray(JSONArray arr) {
        List<Integer> result = new ArrayList<>();
        if (arr == null) {
            return result;
        }
        for (int i = 0; i < arr.length(); i++) {
            result.add(arr.optInt(i, 0));
        }
        return result;
    }

    private void normalizePhysicalDeltas(List<Integer> physicalDeltas, int physicalMoveCount, int displayDeltaMs) {
        while (physicalDeltas.size() > physicalMoveCount) {
            physicalDeltas.remove(physicalDeltas.size() - 1);
        }
        int fallback = Math.max(20, displayDeltaMs / Math.max(1, physicalMoveCount));
        while (physicalDeltas.size() < physicalMoveCount) {
            physicalDeltas.add(fallback);
        }
    }

    private List<Integer> flattenPhysicalMoves() {
        List<Integer> result = new ArrayList<>();
        for (SolveReplayRenderer.MoveStep step : moveSteps) {
            result.addAll(step.physicalMoves);
        }
        return result;
    }

    private void dumpReplayData(String scramble, String movesText, String solveMeta,
                                List<Integer> moveInts, int[] phaseForMove) {
        try {
            java.io.File file = new java.io.File(getActivity().getFilesDir(), "replay_dump.txt");
            java.io.PrintWriter pw = new java.io.PrintWriter(file);
            pw.println("=== REPLAY DUMP ===");
            pw.println("scramble: [" + scramble + "]");
            pw.println("movesText length: " + (movesText != null ? movesText.length() : 0));
            pw.println("moveInts.size: " + moveInts.size());
            for (int i = 0; i < moveInts.size(); i++) {
                int m = moveInts.get(i);
                String phase = phaseForMove != null && i < phaseForMove.length ? String.valueOf(phaseForMove[i]) : "?";
                pw.println("  move[" + i + "]=" + moveToNotation(m) + " phase=" + phase + " code=" + m);
            }
            pw.println("---");
            pw.println("solveMeta length: " + (solveMeta != null ? solveMeta.length() : 0));
            pw.println("solveMeta: " + solveMeta);
            pw.println("--- phases ---");
            for (int p = 0; p < phaseInfos.size(); p++) {
                SolveReplayRenderer.PhaseInfo pi = phaseInfos.get(p);
                pw.println("  Phase[" + p + "] " + pi.name + " moveCount=" + pi.moveCount
                        + " startMs=" + pi.startMs + " endMs=" + pi.endMs);
            }
            pw.println("--- faceletStates ---");
            for (int i = 0; i < faceletStates.size() && i < 15; i++) {
                pw.println("  state[" + i + "]=" + faceletStates.get(i));
            }
            pw.println("totalTimeMs=" + totalTimeMs + " inspectionMs=" + inspectionMs);
            pw.close();
            android.util.Log.w("ReplayDump", "Dumped to " + file.getAbsolutePath());
        } catch (Exception e) {
            android.util.Log.w("ReplayDump", "Dump failed", e);
        }
    }

    private int sumAfterFirst(List<Integer> deltas) {
        int sum = 0;
        for (int i = 1; i < deltas.size(); i++) sum += deltas.get(i);
        return sum > 0 ? sum : 1;
    }

    private List<Integer> parseMovesText(String text) {
        List<Integer> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return result;
        String[] parts = text.trim().split("\\s+");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            int move = notationToMove(part);
            if (move >= 0) result.add(move);
        }
        return result;
    }

    private static final String NOTATION_FACES = "URFDLBEMS";
    private static final String[] NOTATION_SUFFIX = {"", "2", "'"};

    private int notationToMove(String s) {
        // Must be exactly 1 char (e.g. "U") or 2 chars (e.g. "U'", "U2")
        if (s.length() < 1 || s.length() > 2) return -1;
        char face = s.charAt(0);
        int fi = NOTATION_FACES.indexOf(face);
        if (fi < 0) return -1;
        int pow = 0;
        if (s.length() == 2) {
            char suffix = s.charAt(1);
            if (suffix == '2') pow = 1;
            else if (suffix == '\'') pow = 2;
            else return -1; // invalid second character
        }
        return fi * 3 + pow;
    }

    private String moveToNotation(int move) {
        return NOTATION_FACES.charAt(move / 3) + NOTATION_SUFFIX[move % 3];
    }

    // Slice decomposition + rotation correction:
    //   E (≡D): D' + U  + y'     main=D(3), opp=U(0), rot=y'(0,2)
    //   M (≡L): L' + R  + x'     main=L(4), opp=R(1), rot=x'(1,2)
    //   S (≡F): F' + B  + z      main=F(2), opp=B(5), rot=z (2,0)
    private static final int[] SLICE_MAIN  = {3, 4, 2}; // axis 6=E→D, 7=M→L, 8=S→F
    private static final int[] SLICE_OPP   = {0, 1, 5}; // U, R, B
    private static final int[] SLICE_ROT_AXIS = {0, 1, 2}; // y, x, z
    private static final int[] SLICE_ROT_POW  = {2, 2, 0}; // y', x', z

    /**
     * Apply one move to CubieCube, handling slices with rotation correction.
     */
    private CubieCube applyMoveToCube(CubieCube cc, int move) {
        return SliceMoveUtils.applyMove(cc, move);
    }

    /**
     * Apply a cube rotation (y/x/z axis, 0/1/2 power) to a facelet string.
     * Axis: 0=y(U), 1=x(R), 2=z(F). Power: 0=CW, 1=180°, 2=CCW.
     */
    private String rotateFacelet(String facelet, int rotAxis, int pow) {
        if (facelet == null || facelet.length() != 54) return facelet;
        char[] out = new char[54];
        int[] map = getRotationMap(rotAxis, pow);
        for (int i = 0; i < 54; i++) {
            out[i] = facelet.charAt(map[i]);
        }
        return new String(out);
    }

    // --- Facelet rotation maps (precomputed) ---
    // Face order: U(0-8) R(9-17) F(18-26) D(27-35) L(36-44) B(45-53)
    // Each face 3x3 grid in row-major: top-left to bottom-right

    private int[] getRotationMap(int axis, int pow) {
        // Build map by composing 90° steps
        int[] map = identityMap();
        int steps = pow == 0 ? 1 : (pow == 1 ? 2 : 3); // CW=1, 180=2, CCW=3
        for (int s = 0; s < steps; s++) {
            map = applyRot90(map, axis);
        }
        return map;
    }

    private int[] identityMap() {
        int[] m = new int[54];
        for (int i = 0; i < 54; i++) m[i] = i;
        return m;
    }

    /**
     * Apply one 90° CW cube rotation to a permutation map.
     * map[i] = source index for output position i.
     * CW rotation means: what was on FACE_A is now on FACE_B (FACE_A → FACE_B).
     * So output[FACE_B] = input[FACE_A], i.e. map[dst_face*9+i] = src_face*9+r[i].
     */
    private int[] applyRot90(int[] map, int axis) {
        int[] prev = map.clone();
        int[] result = new int[54];
        for (int i = 0; i < 54; i++) result[i] = prev[i];

        if (axis == 0) {
            // y: U-axis CW. F→R, R→B, B→L, L→F. D rotates CW (from above).
            copyFaceRot(result, prev, R_OFF, F_OFF, 0);   // F → R
            copyFaceRot(result, prev, B_OFF, R_OFF, 0);   // R → B
            copyFaceRot(result, prev, L_OFF, B_OFF, 0);   // B → L
            copyFaceRot(result, prev, F_OFF, L_OFF, 0);   // L → F
            copyFaceSelf(result, prev, U_OFF, 0);         // U stays
            copyFaceSelf(result, prev, D_OFF, 1);         // D rotates CW
        } else if (axis == 1) {
            // x: R-axis CW. B→U, U→F, F→D, D→B. L rotates CCW.
            copyFaceRot(result, prev, U_OFF, B_OFF, 0);   // B → U
            copyFaceRot(result, prev, F_OFF, U_OFF, 0);   // U → F
            copyFaceRot(result, prev, D_OFF, F_OFF, 0);   // F → D
            copyFaceRot(result, prev, B_OFF, D_OFF, 0);   // D → B
            copyFaceSelf(result, prev, R_OFF, 0);         // R stays
            copyFaceSelf(result, prev, L_OFF, 2);         // L rotates CCW
        } else {
            // z: F-axis CW. U→R, R→D, D→L, L→U. B rotates CW.
            copyFaceRot(result, prev, R_OFF, U_OFF, 0);   // U → R
            copyFaceRot(result, prev, D_OFF, R_OFF, 0);   // R → D
            copyFaceRot(result, prev, L_OFF, D_OFF, 0);   // D → L
            copyFaceRot(result, prev, U_OFF, L_OFF, 0);   // L → U
            copyFaceSelf(result, prev, F_OFF, 0);         // F stays
            copyFaceSelf(result, prev, B_OFF, 1);         // B rotates CW
        }
        return result;
    }

    // Face offset constants in 54-char facelet string: U=0, R=9, F=18, D=27, L=36, B=45
    private static final int U_OFF = 0, R_OFF = 9, F_OFF = 18, D_OFF = 27, L_OFF = 36, B_OFF = 45;

    private void copyFaceRot(int[] dst, int[] src, int dstOff, int srcOff, int rot) {
        int[][] rotIdx = { {0,1,2,3,4,5,6,7,8}, {6,3,0,7,4,1,8,5,2}, {8,7,6,5,4,3,2,1,0}, {2,5,8,1,4,7,0,3,6} };
        int[] r = rotIdx[rot & 3];
        for (int i = 0; i < 9; i++) {
            dst[dstOff + i] = src[srcOff + r[i]];
        }
    }

    private void copyFaceSelf(int[] dst, int[] src, int off, int rot) {
        copyFaceRot(dst, src, off, off, rot);
    }

    private List<Integer> estimateDeltas(List<Integer> moves, int solveTimeMs) {
        List<Integer> deltas = new ArrayList<>();
        if (moves.isEmpty()) return deltas;

        if (phaseInfos.isEmpty() || solveTimeMs <= 0) {
            int avg = Math.max(80, solveTimeMs / Math.max(1, moves.size()));
            for (int i = 0; i < moves.size(); i++) {
                deltas.add(avg);
            }
        } else {
            int[] phaseMoveCounts = new int[7];
            for (int i = 0; i < moves.size() && i < 200; i++) {
                int phase = findPhaseIndexByMove(i);
                if (phase >= 0 && phase < 7) phaseMoveCounts[phase]++;
            }

            for (int i = 0; i < moves.size(); i++) {
                int phase = findPhaseIndexByMove(i);
                if (phase >= 0 && phase < phaseInfos.size()) {
                    SolveReplayRenderer.PhaseInfo pi = phaseInfos.get(phase);
                    int count = phaseMoveCounts[phase];
                    int avg = count > 0 ? (pi.endMs - pi.startMs) / count : 80;
                    deltas.add(Math.max(10, avg));
                } else {
                    deltas.add(80);
                }
            }
        }
        return deltas;
    }

    private int findPhaseIndexByMove(int moveIdx) {
        if (phaseInfos.isEmpty()) return 0;
        int count = 0;
        for (int p = 0; p < phaseInfos.size(); p++) {
            SolveReplayRenderer.PhaseInfo pi = phaseInfos.get(p);
            int phaseDuration = pi.endMs - pi.startMs;
            int phaseMoves = totalTimeMs > 0 ? (int)((float) phaseDuration / totalTimeMs * moveSteps.size()) : 0;
            phaseMoves = Math.max(1, phaseMoves);
            if (moveIdx < count + phaseMoves) return p;
            count += phaseMoves;
        }
        return phaseInfos.size() - 1;
    }

    private int findPhaseIndex(int cumulativeMs) {
        for (int i = 0; i < phaseInfos.size(); i++) {
            SolveReplayRenderer.PhaseInfo pi = phaseInfos.get(i);
            if (cumulativeMs >= pi.startMs && cumulativeMs < pi.endMs) return i;
        }
        if (!phaseInfos.isEmpty() && cumulativeMs >= phaseInfos.get(phaseInfos.size() - 1).endMs) {
            return phaseInfos.size() - 1;
        }
        return 0;
    }

    private void computeFaceletStates(String scramble) {
        faceletStates.clear();
        stepFaceletStates.clear();
        CubieCube cc = new CubieCube();
        if (!TextUtils.isEmpty(scramble)) {
            List<Integer> scrambleMoves = parseMovesText(scramble);
            for (int m : scrambleMoves) {
                cc = applyMoveToCube(cc, m);
            }
        }
        faceletStates.add(cs.min2phase.Util.toFaceCube(cc));

        for (SolveReplayRenderer.MoveStep step : moveSteps) {
            List<String> states = new ArrayList<>();
            states.add(cs.min2phase.Util.toFaceCube(cc));
            for (int m : step.physicalMoves) {
                cc = applyMoveToCube(cc, m);
                states.add(cs.min2phase.Util.toFaceCube(cc));
            }
            stepFaceletStates.add(states);
            faceletStates.add(cs.min2phase.Util.toFaceCube(cc));
        }
    }

    private void showState(int stepIndex) {
        if (cube3DView == null) return;
        if (stepIndex >= 0 && stepIndex < faceletStates.size()) {
            cube3DView.showCubeState(faceletStates.get(stepIndex));
        }
    }

    private void playNext() {
        if (currentIndex >= moveSteps.size()) return;
        animateDisplayStep(currentIndex, 0);
    }

    private void handleAnimationEnd() {
        if (animatingStepIndex < 0 || animatingStepIndex >= moveSteps.size()) {
            if (isPlaying && currentIndex < moveSteps.size()) {
                playNext();
            }
            return;
        }
        SolveReplayRenderer.MoveStep step = moveSteps.get(animatingStepIndex);
        int nextSubMove = animatingSubMoveIndex + 1;
        if (nextSubMove < step.physicalMoves.size()) {
            animateDisplayStep(animatingStepIndex, nextSubMove);
            return;
        }

        currentIndex = animatingStepIndex + 1;
        animatingStepIndex = -1;
        animatingSubMoveIndex = -1;
        sparklineView.setCurrentStep(currentIndex - 1);
        updateUi();

        if (isPlaying && currentIndex < moveSteps.size()) {
            playNext();
        } else if (currentIndex >= moveSteps.size()) {
            isPlaying = false;
            btnPlay.setText("▶");
            updateUi();
        }
    }

    private void animateDisplayStep(int stepIndex, int subMoveIndex) {
        if (stepIndex < 0 || stepIndex >= moveSteps.size()) {
            return;
        }
        SolveReplayRenderer.MoveStep step = moveSteps.get(stepIndex);
        if (subMoveIndex < 0 || subMoveIndex >= step.physicalMoves.size()) {
            return;
        }
        List<String> states = stepIndex < stepFaceletStates.size() ? stepFaceletStates.get(stepIndex) : null;
        if (states == null || subMoveIndex + 1 >= states.size()) {
            showState(stepIndex + 1);
            currentIndex = stepIndex + 1;
            sparklineView.setCurrentStep(stepIndex);
            updateUi();
            return;
        }
        animatingStepIndex = stepIndex;
        animatingSubMoveIndex = subMoveIndex;
        sparklineView.setCurrentStep(stepIndex);
        updateUiForStep(step);
        int delta = subMoveIndex < step.physicalDeltas.size() ? step.physicalDeltas.get(subMoveIndex)
                : Math.max(20, step.deltaMs / Math.max(1, step.physicalMoves.size()));
        long duration = Math.max(20, (long) (delta / speed));
        animateMoveSafe(states.get(subMoveIndex), states.get(subMoveIndex + 1),
                step.physicalMoves.get(subMoveIndex), duration);
    }

    private void animateMoveSafe(String from, String to, int move, long duration) {
        if (cube3DView == null) return;
        if (move >= 0 && move < 27) {
            cube3DView.animateMove(from, to, move, duration);
        } else {
            cube3DView.showCubeState(to);
            handleAnimationEnd();
        }
    }

    private void jumpToStep(int step) {
        isPlaying = false;
        btnPlay.setText("▶");
        animatingStepIndex = -1;
        animatingSubMoveIndex = -1;
        currentIndex = Math.max(0, Math.min(step, moveSteps.size()));
        showState(currentIndex);
        sparklineView.setCurrentStep(Math.min(currentIndex, moveSteps.size() - 1));
        updateUi();
    }

    private void stepNext() {
        if (currentIndex < moveSteps.size()) {
            isPlaying = false;
            btnPlay.setText("▶");
            animateDisplayStep(currentIndex, 0);
        }
    }

    private void stepPrev() {
        if (currentIndex > 0) {
            animatingStepIndex = -1;
            animatingSubMoveIndex = -1;
            currentIndex--;
            showState(currentIndex);
            sparklineView.setCurrentStep(Math.max(0, currentIndex - 1));
            updateUi();
        }
    }

    private void togglePlay() {
        if (moveSteps.isEmpty()) return;
        if (isPlaying) {
            isPlaying = false;
            btnPlay.setText("▶");
        } else {
            if (currentIndex >= moveSteps.size()) {
                currentIndex = 0;
                showState(0);
                sparklineView.setCurrentStep(0);
            }
            animatingStepIndex = -1;
            animatingSubMoveIndex = -1;
            isPlaying = true;
            btnPlay.setText("⏸");
            playNext();
        }
    }

    private void applyReplayBackground(View root) {
        ImageView backgroundView = root.findViewById(R.id.iv_replay_background);
        if (backgroundView == null) {
            root.setBackgroundColor(APP.getBackgroundColor());
            return;
        }
        if (APP.useBgcolor) {
            backgroundView.setVisibility(View.GONE);
            root.setBackgroundColor(APP.getBackgroundColor());
            return;
        }

        Bitmap snapshot = captureMainBackground();
        if (snapshot != null) {
            backgroundView.setImageBitmap(snapshot);
            backgroundView.setVisibility(View.VISIBLE);
            root.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        if (getContext() != null) {
            try {
                Bitmap bitmap = Utils.getBitmap(getContext(), APP.dm, APP.picUri, APP.picPath);
                backgroundView.setImageBitmap(bitmap);
                backgroundView.setVisibility(View.VISIBLE);
                root.setBackgroundColor(Color.TRANSPARENT);
                return;
            } catch (Exception ignored) {
            }
        }

        backgroundView.setVisibility(View.GONE);
        root.setBackgroundColor(APP.getBackgroundColor());
    }

    private Bitmap captureMainBackground() {
        if (getActivity() == null) {
            return null;
        }
        View mainLayout = getActivity().findViewById(R.id.main_layout);
        if (mainLayout == null || mainLayout.getWidth() <= 0 || mainLayout.getHeight() <= 0) {
            return null;
        }
        Drawable mainBackground = mainLayout.getBackground();
        if (mainBackground == null) {
            return null;
        }
        Drawable drawable = copyDrawable(mainBackground);
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(mainLayout.getWidth(), mainLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Drawable copyDrawable(Drawable drawable) {
        Drawable.ConstantState state = drawable.getConstantState();
        if (state != null) {
            return state.newDrawable(getResources()).mutate();
        }
        return drawable.getCurrent();
    }

    private void applyReplayColors(@Nullable View divider) {
        int primaryTextColor = APP.getTextColor();
        int secondaryTextColor = withAlpha(primaryTextColor, 0.72f);
        tvMoveInfo.setTextColor(primaryTextColor);
        tvStats.setTextColor(secondaryTextColor);
        if (divider != null) {
            divider.setBackgroundColor(withAlpha(primaryTextColor, 0.22f));
        }
        sparklineView.setThemeColors(primaryTextColor, secondaryTextColor);
    }

    private int withAlpha(int color, float alphaFraction) {
        int alpha = Math.max(0, Math.min(255, Math.round(255 * alphaFraction)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void cycleSpeed() {
        if (speed == 1.0f) speed = 2.0f;
        else if (speed == 2.0f) speed = 0.5f;
        else speed = 1.0f;
        btnSpeed.setText("Speed: " + (speed == 1.0f ? "1x" : speed == 2.0f ? "2x" : "0.5x"));
    }

    private void updateUi() {
        if (currentIndex > 0 && currentIndex <= moveSteps.size()) {
            SolveReplayRenderer.MoveStep step = moveSteps.get(currentIndex - 1);
            updateUiForStep(step);
        } else if (currentIndex == 0 && !moveSteps.isEmpty()) {
            tvMoveInfo.setText("Inspection\n" + formatMs(inspectionMs));
        } else {
            tvMoveInfo.setText("");
        }

        int totalMoves = moveSteps.size();
        float avgTps = totalTimeMs > 0 ? totalMoves * 1000f / Math.max(1, totalTimeMs - inspectionMs) : 0;
        float inspectPct = totalTimeMs > 0 ? inspectionMs * 100f / totalTimeMs : 0;
        tvStats.setText(totalMoves + " moves\nAvg TPS " + String.format("%.1f", avgTps)
                + "\nTotal " + formatMs(totalTimeMs)
                + "\nInsp " + String.format("%.0f", inspectPct) + "%");
    }

    private void updateUiForStep(SolveReplayRenderer.MoveStep step) {
        float tps = step.deltaMs > 0 ? 1000f / step.deltaMs : 0;
        tvMoveInfo.setText("▶ " + step.notation + "\ntime " + formatMs(step.deltaMs)
                + "\nTPS " + String.format("%.1f", tps));
    }

    private String formatMs(int ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 10000) return String.format("%.2fs", ms / 1000f);
        return String.format("%.1fs", ms / 1000f);
    }

    private int getPhaseColor(int index) {
        int[] colors = {0xFF9E9E9E, 0xFF4CAF50, 0xFF66BB6A, 0xFF81C784, 0xFFA5D6A7, 0xFFFF9800, 0xFF2196F3};
        if (index < 0 || index >= colors.length) return 0xFFBDBDBD;
        return colors[index];
    }
}
