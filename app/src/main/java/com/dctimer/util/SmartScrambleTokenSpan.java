package com.dctimer.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SmartScrambleTokenSpan extends ReplacementSpan {
    private final String displayText;
    private final String widthReference;
    private final int textColor;
    private final int backgroundColor;
    private final float horizontalPaddingPx;
    private final float verticalPaddingPx;
    private final float cornerRadiusPx;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SmartScrambleTokenSpan(String displayText, String widthReference, int textColor, int backgroundColor,
                                  float horizontalPaddingPx, float verticalPaddingPx, float cornerRadiusPx) {
        this.displayText = displayText;
        this.widthReference = widthReference;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.horizontalPaddingPx = horizontalPaddingPx;
        this.verticalPaddingPx = verticalPaddingPx;
        this.cornerRadiusPx = cornerRadiusPx;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        if (fm != null) {
            paint.getFontMetricsInt(fm);
        }
        return Math.round(paint.measureText(widthReference) + horizontalPaddingPx * 2f);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
                     int bottom, @NonNull Paint paint) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float referenceWidth = paint.measureText(widthReference);
        float drawWidth = referenceWidth + horizontalPaddingPx * 2f;
        if ((backgroundColor >>> 24) != 0) {
            backgroundPaint.setColor(backgroundColor);
            RectF rect = new RectF(
                    x,
                    y + fontMetrics.ascent - verticalPaddingPx,
                    x + drawWidth,
                    y + fontMetrics.descent + verticalPaddingPx
            );
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, backgroundPaint);
        }

        int previousColor = paint.getColor();
        Paint.Style previousStyle = paint.getStyle();
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(displayText, x + horizontalPaddingPx, y, paint);
        paint.setColor(previousColor);
        paint.setStyle(previousStyle);
    }
}
