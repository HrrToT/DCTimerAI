package com.dctimer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SolveReplayRenderer extends View {
    private static final int[] PHASE_COLORS = {
            0xFF9E9E9E, // Cross - gray
            0xFF4CAF50, // F2L 1 - green
            0xFF66BB6A, // F2L 2
            0xFF81C784, // F2L 3
            0xFFA5D6A7, // F2L 4
            0xFFFF9800, // OLL - orange
            0xFF2196F3, // PLL - blue
    };

    private static final int MAX_SPARK_HEIGHT_LEVELS = 8;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ratioPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private int primaryTextColor = Color.WHITE;
    private int secondaryTextColor = 0xFFBDBDBD;

    private final List<MoveStep> moveSteps = new ArrayList<>();
    private final List<PhaseInfo> phaseInfos = new ArrayList<>();
    private int totalTimeMs;
    private int inspectionMs;
    private int currentStep = -1;
    private OnStepClickListener stepClickListener;

    public static class MoveStep {
        public final int index;
        public final String notation;
        public final int deltaMs;
        public final int cumulativeMs;
        public final int phaseIndex;
        public final List<Integer> physicalMoves;
        public final List<Integer> physicalDeltas;

        public MoveStep(int index, String notation, int deltaMs, int cumulativeMs, int phaseIndex) {
            this(index, notation, deltaMs, cumulativeMs, phaseIndex,
                    new ArrayList<Integer>(), new ArrayList<Integer>());
        }

        public MoveStep(int index, String notation, int deltaMs, int cumulativeMs, int phaseIndex,
                        List<Integer> physicalMoves, List<Integer> physicalDeltas) {
            this.index = index;
            this.notation = notation;
            this.deltaMs = deltaMs;
            this.cumulativeMs = cumulativeMs;
            this.phaseIndex = phaseIndex;
            this.physicalMoves = new ArrayList<>(physicalMoves);
            this.physicalDeltas = new ArrayList<>(physicalDeltas);
        }
    }

    public static class PhaseInfo {
        public final String name;
        public final int startMs;
        public final int endMs;
        public final int color;
        public final int moveCount;

        public PhaseInfo(String name, int startMs, int endMs, int color) {
            this(name, startMs, endMs, color, 0);
        }

        public PhaseInfo(String name, int startMs, int endMs, int color, int moveCount) {
            this.name = name;
            this.startMs = startMs;
            this.endMs = endMs;
            this.color = color;
            this.moveCount = moveCount;
        }
    }

    public interface OnStepClickListener {
        void onStepClick(int stepIndex);
    }

    public SolveReplayRenderer(Context context) {
        super(context);
        init();
    }

    public SolveReplayRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setColor(primaryTextColor);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        indicatorPaint.setColor(primaryTextColor);
        indicatorPaint.setStyle(Paint.Style.FILL);
        indicatorPaint.setStrokeWidth(3f);
        float density = getResources().getDisplayMetrics().density;
        setMinimumHeight((int) (72f * density));
    }

    public void setData(List<MoveStep> steps, List<PhaseInfo> phases, int totalTimeMs, int inspectionMs) {
        moveSteps.clear();
        moveSteps.addAll(steps);
        phaseInfos.clear();
        phaseInfos.addAll(phases);
        this.totalTimeMs = totalTimeMs;
        this.inspectionMs = inspectionMs;
        currentStep = -1;
        invalidate();
    }

    public void setCurrentStep(int step) {
        currentStep = step;
        invalidate();
    }

    public void setOnStepClickListener(OnStepClickListener listener) {
        stepClickListener = listener;
    }

    public void setThemeColors(int textColor, int subTextColor) {
        primaryTextColor = textColor;
        secondaryTextColor = subTextColor;
        textPaint.setColor(textColor);
        indicatorPaint.setColor(textColor);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float contentLeft = getPaddingLeft();
        float contentTop = getPaddingTop();
        float contentBottom = getPaddingTop() + h;
        float topGap = 4f * density;
        float sectionGap = 6f * density;
        float textGap = 4f * density;
        float bottomGap = 4f * density;

        float ratioBarHeight = clamp(h * 0.16f, 10f * density, 20f * density);
        float ratioBarW = w * 0.92f;
        float ratioBarX = contentLeft + (w - ratioBarW) / 2f;
        float ratioBarY = contentTop + topGap;
        float summaryTextSize = clamp(h * 0.12f, 10f * density, 15f * density);
        float phaseLabelTextSize = clamp(h * 0.12f, 10f * density, 15f * density);
        float textBaseline = ratioBarY + ratioBarHeight + textGap + summaryTextSize;

        // --- Ratio bar (top area) ---
        float inspectionFrac = totalTimeMs > 0 ? (float) inspectionMs / totalTimeMs : 0f;

        // Inspection portion
        ratioPaint.setColor(0xFF424242);
        canvas.drawRect(ratioBarX, ratioBarY, ratioBarX + ratioBarW * inspectionFrac, ratioBarY + ratioBarHeight, ratioPaint);

        // Turning portion
        ratioPaint.setColor(0xFF4CAF50);
        canvas.drawRect(ratioBarX + ratioBarW * inspectionFrac, ratioBarY, ratioBarX + ratioBarW, ratioBarY + ratioBarHeight, ratioPaint);

        // Ratio text
        textPaint.setColor(secondaryTextColor);
        textPaint.setTextSize(summaryTextSize);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("INSP " + formatMs(inspectionMs) + " (" + (int)(inspectionFrac * 100) + "%)",
                ratioBarX, textBaseline, textPaint);
        String turningText = "TURN " + formatMs(totalTimeMs - inspectionMs) + " (" + (int)((1 - inspectionFrac) * 100) + "%)";
        float turningStart = ratioBarX + ratioBarW * inspectionFrac + 6f * density;
        float minTurningStart = ratioBarX + ratioBarW * 0.52f;
        canvas.drawText(turningText, Math.max(turningStart, minTurningStart), textBaseline, textPaint);

        // --- Sparkline (middle area) ---
        float sparklineTop = textBaseline + sectionGap;
        float labelBaseY = contentBottom - bottomGap;
        float sparklineBottom = labelBaseY - phaseLabelTextSize - 4f * density;
        float sparklineHeight = Math.max(24f * density, sparklineBottom - sparklineTop);
        float sparklineWidth = ratioBarW;

        if (moveSteps.isEmpty()) return;

        int n = moveSteps.size();
        float barWidth = Math.max(2f, sparklineWidth / n - 1f);
        float stepWidth = sparklineWidth / n;

        // Draw phase backgrounds
        int prevPhase = -1;
        int phaseStartX = 0;
        for (int i = 0; i < n; i++) {
            MoveStep step = moveSteps.get(i);
            if (step.phaseIndex != prevPhase) {
                if (prevPhase >= 0) {
                    float left = ratioBarX + phaseStartX * stepWidth;
                    float right = ratioBarX + i * stepWidth;
                    int color = getPhaseColor(prevPhase);
                    barPaint.setColor((color & 0x00FFFFFF) | 0x18000000);
                    canvas.drawRect(left, sparklineTop, right, sparklineTop + sparklineHeight, barPaint);
                }
                prevPhase = step.phaseIndex;
                phaseStartX = i;
            }
        }
        if (prevPhase >= 0) {
            float left = ratioBarX + phaseStartX * stepWidth;
            float right = ratioBarX + n * stepWidth;
            int color = getPhaseColor(prevPhase);
            barPaint.setColor((color & 0x00FFFFFF) | 0x18000000);
            canvas.drawRect(left, sparklineTop, right, sparklineTop + sparklineHeight, barPaint);
        }

        // Draw sparkline bars
        for (int i = 0; i < n; i++) {
            MoveStep step = moveSteps.get(i);
            int level = (int) (Math.log(Math.max(step.deltaMs, 5) / 5.0) / Math.log(2));
            level = Math.min(level, MAX_SPARK_HEIGHT_LEVELS - 1);
            level = Math.max(level, 0);
            float barHeight = sparklineHeight * (level + 1) / (float) MAX_SPARK_HEIGHT_LEVELS;

            float x = ratioBarX + i * stepWidth;
            barPaint.setColor(getPhaseColor(step.phaseIndex));
            canvas.drawRect(x, sparklineTop + sparklineHeight - barHeight,
                    x + barWidth, sparklineTop + sparklineHeight, barPaint);
        }

        // Phase labels below sparkline
        float labelY = labelBaseY;
        textPaint.setTextSize(phaseLabelTextSize);
        int lastPhaseEnd = -1;
        prevPhase = -1;
        for (int i = 0; i < n; i++) {
            MoveStep step = moveSteps.get(i);
            if (step.phaseIndex != prevPhase && i > lastPhaseEnd) {
                float labelX = ratioBarX + i * stepWidth + stepWidth * 0.5f * countPhaseSteps(i);
                textPaint.setColor(getPhaseColor(step.phaseIndex));
                textPaint.setTextAlign(Paint.Align.CENTER);
                String label = phaseInfos.size() > step.phaseIndex ? phaseInfos.get(step.phaseIndex).name : "";
                canvas.drawText(label, labelX, labelY, textPaint);
                prevPhase = step.phaseIndex;
                lastPhaseEnd = i + countPhaseSteps(i) - 1;
            }
        }

        // --- Current position indicator ---
        if (currentStep >= 0 && currentStep < n) {
            float indicatorX = ratioBarX + currentStep * stepWidth + barWidth / 2f;
            indicatorPaint.setColor(primaryTextColor);
            indicatorPaint.setStyle(Paint.Style.FILL);
            float triSize = clamp(h * 0.04f, 6f * density, 10f * density);
            android.graphics.Path triPath = new android.graphics.Path();
            triPath.moveTo(indicatorX - triSize, sparklineTop - 4f);
            triPath.lineTo(indicatorX + triSize, sparklineTop - 4f);
            triPath.lineTo(indicatorX, sparklineTop + 2f);
            triPath.close();
            canvas.drawPath(triPath, indicatorPaint);
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int countPhaseSteps(int startIndex) {
        int targetPhase = moveSteps.get(startIndex).phaseIndex;
        int count = 0;
        for (int i = startIndex; i < moveSteps.size(); i++) {
            if (moveSteps.get(i).phaseIndex == targetPhase) count++;
            else break;
        }
        return count;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !moveSteps.isEmpty()) {
            float w = getWidth() - getPaddingLeft() - getPaddingRight();
            float ratioBarW = w * 0.9f;
            float x = event.getX() - (getPaddingLeft() + (w - ratioBarW) / 2f);
            int n = moveSteps.size();
            int index = (int) (x / (ratioBarW / n));
            if (index >= 0 && index < n && stepClickListener != null) {
                stepClickListener.onStepClick(index);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private int getPhaseColor(int phaseIndex) {
        if (phaseIndex < 0) return 0xFFBDBDBD;
        if (phaseIndex >= PHASE_COLORS.length) return 0xFFBDBDBD;
        return PHASE_COLORS[phaseIndex];
    }

    private String formatMs(int ms) {
        if (ms < 1000) return ms + "ms";
        return String.format("%.2fs", ms / 1000.0);
    }
}
