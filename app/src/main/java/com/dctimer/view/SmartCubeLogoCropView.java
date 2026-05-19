package com.dctimer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.dctimer.util.SmartCubeLogoProvider;

public class SmartCubeLogoCropView extends View {
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path overlayPath = new Path();
    private final Matrix drawMatrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;

    private Bitmap sourceBitmap;
    private final RectF cropCircleBounds = new RectF();
    private float baseScale = 1f;
    private float userScale = 1f;
    private float translateX;
    private float translateY;
    private float lastTouchX;
    private float lastTouchY;
    private boolean dragging;

    public SmartCubeLogoCropView(Context context) {
        this(context, null);
    }

    public SmartCubeLogoCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        overlayPaint.setColor(0xAA101317);
        overlayPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(0xFFF5F7FA);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f * getResources().getDisplayMetrics().density);
        guidePaint.setColor(0x55FFFFFF);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(1.5f * getResources().getDisplayMetrics().density);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setBitmap(Bitmap bitmap) {
        sourceBitmap = bitmap;
        userScale = 1f;
        if (getWidth() > 0 && getHeight() > 0) {
            configureInitialTransform();
        }
        invalidate();
    }

    public Bitmap exportCroppedLogo() {
        if (sourceBitmap == null || cropCircleBounds.width() <= 0f) {
            return null;
        }
        int size = SmartCubeLogoProvider.LOGO_SIZE_PX;
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, maskPaint);

        Paint exportPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        exportPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        float factor = size / cropCircleBounds.width();
        Matrix exportMatrix = new Matrix();
        exportMatrix.postScale(getTotalScale() * factor, getTotalScale() * factor);
        exportMatrix.postTranslate((getDrawLeft() - cropCircleBounds.left) * factor,
                (getDrawTop() - cropCircleBounds.top) * factor);
        canvas.drawBitmap(sourceBitmap, exportMatrix, exportPaint);
        exportPaint.setXfermode(null);
        return output;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = Math.min(w, h) * 0.08f;
        float diameter = Math.min(w, h) - padding * 2f;
        cropCircleBounds.set((w - diameter) / 2f, (h - diameter) / 2f,
                (w + diameter) / 2f, (h + diameter) / 2f);
        configureInitialTransform();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sourceBitmap != null) {
            updateDrawMatrix();
            canvas.drawBitmap(sourceBitmap, drawMatrix, bitmapPaint);
        }

        overlayPath.reset();
        overlayPath.setFillType(Path.FillType.EVEN_ODD);
        overlayPath.addRect(0f, 0f, getWidth(), getHeight(), Path.Direction.CW);
        overlayPath.addOval(cropCircleBounds, Path.Direction.CCW);
        canvas.drawPath(overlayPath, overlayPaint);
        canvas.drawOval(cropCircleBounds, borderPaint);

        float cx = cropCircleBounds.centerX();
        float cy = cropCircleBounds.centerY();
        canvas.drawLine(cropCircleBounds.left, cy, cropCircleBounds.right, cy, guidePaint);
        canvas.drawLine(cx, cropCircleBounds.top, cx, cropCircleBounds.bottom, guidePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sourceBitmap == null) {
            return false;
        }
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && dragging) {
                    translateX += event.getX() - lastTouchX;
                    translateY += event.getY() - lastTouchY;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    clampTranslation();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void configureInitialTransform() {
        if (sourceBitmap == null || cropCircleBounds.width() <= 0f) {
            return;
        }
        baseScale = Math.max(cropCircleBounds.width() / sourceBitmap.getWidth(),
                cropCircleBounds.height() / sourceBitmap.getHeight());
        userScale = 1f;
        translateX = cropCircleBounds.centerX() - sourceBitmap.getWidth() * baseScale / 2f;
        translateY = cropCircleBounds.centerY() - sourceBitmap.getHeight() * baseScale / 2f;
        clampTranslation();
        invalidate();
    }

    private void updateDrawMatrix() {
        drawMatrix.reset();
        float scale = getTotalScale();
        drawMatrix.postScale(scale, scale);
        drawMatrix.postTranslate(translateX, translateY);
    }

    private float getTotalScale() {
        return baseScale * userScale;
    }

    private float getDrawLeft() {
        return translateX;
    }

    private float getDrawTop() {
        return translateY;
    }

    private void clampTranslation() {
        if (sourceBitmap == null) {
            return;
        }
        float scale = getTotalScale();
        float drawnWidth = sourceBitmap.getWidth() * scale;
        float drawnHeight = sourceBitmap.getHeight() * scale;
        float minX = cropCircleBounds.right - drawnWidth;
        float maxX = cropCircleBounds.left;
        float minY = cropCircleBounds.bottom - drawnHeight;
        float maxY = cropCircleBounds.top;
        if (minX > maxX) {
            translateX = cropCircleBounds.centerX() - drawnWidth / 2f;
        } else {
            translateX = Math.max(minX, Math.min(maxX, translateX));
        }
        if (minY > maxY) {
            translateY = cropCircleBounds.centerY() - drawnHeight / 2f;
        } else {
            translateY = Math.max(minY, Math.min(maxY, translateY));
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (sourceBitmap == null) {
                return false;
            }
            float oldScale = userScale;
            userScale *= detector.getScaleFactor();
            userScale = Math.max(1f, Math.min(6f, userScale));
            float totalOldScale = baseScale * oldScale;
            float totalNewScale = getTotalScale();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            if (totalOldScale > 0f) {
                translateX = focusX - (focusX - translateX) * (totalNewScale / totalOldScale);
                translateY = focusY - (focusY - translateY) * (totalNewScale / totalOldScale);
            }
            clampTranslation();
            invalidate();
            return true;
        }
    }
}
