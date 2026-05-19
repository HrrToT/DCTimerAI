package com.dctimer.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;

import com.dctimer.APP;
import com.dctimer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SmartCubeLogoProvider {
    public static final int MODE_NONE = 0;
    public static final int MODE_BUILTIN = 1;
    public static final int MODE_CUSTOM = 2;

    public static final String BUILTIN_DCTIMER_AI = "dctimer_ai";
    public static final String BUILTIN_HTT = "htt";
    public static final String BUILTIN_CUBE = "cube";
    public static final int CUSTOM_LOGO_MAX_COUNT = 6;

    public static final int LOGO_SIZE_PX = 256;
    private static final int CUSTOM_DECODE_TARGET_PX = 768;
    private static final int CROP_DECODE_TARGET_PX = 1536;
    private static final int PRIMARY_COLOR = 0xff21242a;
    private static final int SECONDARY_COLOR = 0xff5d6670;
    private static final int ACCENT_COLOR = 0xff0d0f13;
    private static final String PREF_NEXT_CUSTOM_SLOT = "smartcubelogonextslot";
    private static final String CUSTOM_FILE_PREFIX = "custom_logo_";
    private static final String CUSTOM_FILE_SUFFIX = ".png";
    private static final String LEGACY_CUSTOM_FILE_NAME = "custom_logo.png";
    private static final Map<String, Bitmap> BUILTIN_LOGO_CACHE = new HashMap<>();
    private static final Map<String, PreviewCacheEntry> CUSTOM_LOGO_CACHE = new HashMap<>();
    private static Bitmap nonePreviewBitmap;
    private static Bitmap uploadPreviewBitmap;

    public static final class CustomLogoEntry {
        public final int slot;
        public final String uri;

        public CustomLogoEntry(int slot, String uri) {
            this.slot = slot;
            this.uri = uri;
        }
    }

    public static final class SavedCustomLogo {
        public final int slot;
        public final String uri;

        public SavedCustomLogo(int slot, String uri) {
            this.slot = slot;
            this.uri = uri;
        }
    }

    private static final class PreviewCacheEntry {
        final long lastModified;
        final Bitmap bitmap;

        PreviewCacheEntry(long lastModified, Bitmap bitmap) {
            this.lastModified = lastModified;
            this.bitmap = bitmap;
        }
    }

    private SmartCubeLogoProvider() {
    }

    public static Bitmap loadLogoBitmap(Context context) {
        if (context == null) {
            return null;
        }
        if (APP.smartCubeLogoMode == MODE_CUSTOM) {
            try {
                Bitmap customBitmap = loadCustomLogoPreviewBitmap(context, APP.smartCubeLogoUri);
                if (customBitmap != null) {
                    return customBitmap;
                }
            } catch (IOException ignored) {
            }
        } else if (APP.smartCubeLogoMode == MODE_BUILTIN) {
            return loadBuiltinLogoBitmap(APP.smartCubeLogoBuiltinId);
        } else {
            return null;
        }
        if (!TextUtils.isEmpty(APP.smartCubeLogoBuiltinId) && APP.smartCubeLogoMode == MODE_BUILTIN) {
            return loadBuiltinLogoBitmap(APP.smartCubeLogoBuiltinId);
        }
        return null;
    }

    public static Bitmap loadCustomLogoBitmap(Context context, String uriText) throws IOException {
        if (context == null || TextUtils.isEmpty(uriText)) {
            return null;
        }
        Uri uri = Uri.parse(uriText);
        Bitmap source = decodeCustomSource(context.getContentResolver(), uri);
        if (source == null) {
            throw new IOException("decode bitmap failed");
        }
        Bitmap result = normalizeCustomLogo(source);
        source.recycle();
        return result;
    }

    public static boolean hasCustomLogo() {
        return !TextUtils.isEmpty(APP.smartCubeLogoUri);
    }

    public static boolean hasAnyCustomLogo(Context context) {
        return !getCustomLogoEntries(context).isEmpty();
    }

    public static Bitmap loadCropSourceBitmap(Context context, String uriText) throws IOException {
        if (context == null || TextUtils.isEmpty(uriText)) {
            return null;
        }
        return decodeBitmap(context.getContentResolver(), Uri.parse(uriText), CROP_DECODE_TARGET_PX);
    }

    public static SavedCustomLogo saveNextCustomLogoBitmap(Context context, Bitmap bitmap) throws IOException {
        if (context == null || bitmap == null) {
            throw new IOException("invalid logo bitmap");
        }
        int slot = getNextCustomLogoSlot(context);
        String uri = saveCustomLogoBitmap(context, bitmap, slot);
        removeCustomLogoCache(uri);
        advanceNextCustomLogoSlot(context, slot);
        return new SavedCustomLogo(slot, uri);
    }

    public static String saveCustomLogoBitmap(Context context, Bitmap bitmap, int slot) throws IOException {
        if (context == null || bitmap == null) {
            throw new IOException("invalid logo bitmap");
        }
        File dir = new File(context.getFilesDir(), "smart_cube_logo");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("create logo directory failed");
        }
        File file = new File(dir, getCustomLogoFileName(slot));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, false);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("compress logo failed");
            }
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
        return Uri.fromFile(file).toString();
    }

    public static List<CustomLogoEntry> getCustomLogoEntries(Context context) {
        ArrayList<CustomLogoEntry> entries = new ArrayList<>();
        if (context == null) {
            return entries;
        }
        ensureLegacyCustomLogoMigrated(context);
        for (int slot = 1; slot <= CUSTOM_LOGO_MAX_COUNT; slot++) {
            File file = getCustomLogoFile(context, slot);
            if (file.exists() && file.isFile()) {
                entries.add(new CustomLogoEntry(slot, Uri.fromFile(file).toString()));
            }
        }
        return entries;
    }

    public static int resolveCustomLogoSlot(String uriText) {
        if (TextUtils.isEmpty(uriText)) {
            return -1;
        }
        String lower = uriText.toLowerCase();
        int start = lower.lastIndexOf(CUSTOM_FILE_PREFIX);
        int end = lower.lastIndexOf(CUSTOM_FILE_SUFFIX);
        if (start < 0 || end <= start) {
            return -1;
        }
        String number = lower.substring(start + CUSTOM_FILE_PREFIX.length(), end);
        try {
            int slot = Integer.parseInt(number);
            return slot >= 1 && slot <= CUSTOM_LOGO_MAX_COUNT ? slot : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String getCustomLogoLabel(Context context, int slot) {
        return context.getString(R.string.smart_cube_logo_custom_slot, slot);
    }

    public static Bitmap createNonePreviewBitmap() {
        if (nonePreviewBitmap != null && !nonePreviewBitmap.isRecycled()) {
            return nonePreviewBitmap;
        }
        Bitmap bitmap = createLogoCanvas();
        Canvas canvas = new Canvas(bitmap);
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(SECONDARY_COLOR);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(10f);
        canvas.drawCircle(LOGO_SIZE_PX / 2f, LOGO_SIZE_PX / 2f, 86f, ringPaint);

        Paint slashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slashPaint.setColor(PRIMARY_COLOR);
        slashPaint.setStrokeCap(Paint.Cap.ROUND);
        slashPaint.setStrokeWidth(16f);
        canvas.drawLine(74f, 182f, 182f, 74f, slashPaint);
        nonePreviewBitmap = bitmap;
        return nonePreviewBitmap;
    }

    public static Bitmap createUploadPreviewBitmap() {
        if (uploadPreviewBitmap != null && !uploadPreviewBitmap.isRecycled()) {
            return uploadPreviewBitmap;
        }
        Bitmap bitmap = createLogoCanvas();
        Canvas canvas = new Canvas(bitmap);
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(SECONDARY_COLOR);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(10f);
        canvas.drawCircle(LOGO_SIZE_PX / 2f, LOGO_SIZE_PX / 2f, 86f, ringPaint);

        Paint plusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        plusPaint.setColor(PRIMARY_COLOR);
        plusPaint.setStrokeCap(Paint.Cap.ROUND);
        plusPaint.setStrokeWidth(16f);
        canvas.drawLine(128f, 74f, 128f, 182f, plusPaint);
        canvas.drawLine(74f, 128f, 182f, 128f, plusPaint);
        uploadPreviewBitmap = bitmap;
        return uploadPreviewBitmap;
    }

    public static Bitmap loadCustomLogoPreviewBitmap(Context context, String uriText) throws IOException {
        if (context == null || TextUtils.isEmpty(uriText)) {
            return null;
        }
        File file = resolveCustomLogoFile(context, uriText);
        if (file == null || !file.exists() || !file.isFile()) {
            return loadCustomLogoBitmap(context, uriText);
        }
        String cacheKey = file.getAbsolutePath();
        long lastModified = file.lastModified();
        PreviewCacheEntry cached = CUSTOM_LOGO_CACHE.get(cacheKey);
        if (cached != null && cached.bitmap != null && !cached.bitmap.isRecycled()
                && cached.lastModified == lastModified) {
            return cached.bitmap;
        }
        Bitmap bitmap = loadCustomLogoBitmap(context, uriText);
        if (bitmap != null) {
            CUSTOM_LOGO_CACHE.put(cacheKey, new PreviewCacheEntry(lastModified, bitmap));
        }
        return bitmap;
    }

    public static Bitmap loadBuiltinLogoBitmap(String builtinId) {
        String safeId = TextUtils.isEmpty(builtinId) ? BUILTIN_DCTIMER_AI : builtinId;
        Bitmap cached = BUILTIN_LOGO_CACHE.get(safeId);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap bitmap = createBuiltinLogo(safeId);
        BUILTIN_LOGO_CACHE.put(safeId, bitmap);
        return bitmap;
    }

    public static void clearLogoData(Context context) {
        if (context == null) {
            return;
        }
        context.getSharedPreferences("dctimer", Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_NEXT_CUSTOM_SLOT)
                .apply();
        File dir = new File(context.getFilesDir(), "smart_cube_logo");
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file != null && file.exists()) {
                    removeCustomLogoCache(Uri.fromFile(file).toString());
                    // Best-effort cleanup; reset should continue even if one file cannot be removed.
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    private static Bitmap decodeCustomSource(ContentResolver resolver, Uri uri) throws IOException {
        return decodeBitmap(resolver, uri, CUSTOM_DECODE_TARGET_PX);
    }

    private static File resolveCustomLogoFile(Context context, String uriText) {
        if (context == null || TextUtils.isEmpty(uriText)) {
            return null;
        }
        try {
            Uri uri = Uri.parse(uriText);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                if (!TextUtils.isEmpty(path)) {
                    return new File(path);
                }
            }
        } catch (Exception ignored) {
        }
        int slot = resolveCustomLogoSlot(uriText);
        if (slot > 0) {
            return getCustomLogoFile(context, slot);
        }
        return null;
    }

    private static void removeCustomLogoCache(String uriText) {
        if (TextUtils.isEmpty(uriText)) {
            return;
        }
        try {
            Uri uri = Uri.parse(uriText);
            if ("file".equalsIgnoreCase(uri.getScheme()) && !TextUtils.isEmpty(uri.getPath())) {
                CUSTOM_LOGO_CACHE.remove(new File(uri.getPath()).getAbsolutePath());
                return;
            }
        } catch (Exception ignored) {
        }
    }

    private static int getNextCustomLogoSlot(Context context) {
        int slot = context.getSharedPreferences("dctimer", Context.MODE_PRIVATE)
                .getInt(PREF_NEXT_CUSTOM_SLOT, 1);
        if (slot < 1 || slot > CUSTOM_LOGO_MAX_COUNT) {
            slot = 1;
        }
        return slot;
    }

    private static void advanceNextCustomLogoSlot(Context context, int currentSlot) {
        int nextSlot = currentSlot + 1;
        if (nextSlot > CUSTOM_LOGO_MAX_COUNT) {
            nextSlot = 1;
        }
        context.getSharedPreferences("dctimer", Context.MODE_PRIVATE)
                .edit()
                .putInt(PREF_NEXT_CUSTOM_SLOT, nextSlot)
                .apply();
    }

    private static File getCustomLogoFile(Context context, int slot) {
        File dir = new File(context.getFilesDir(), "smart_cube_logo");
        return new File(dir, getCustomLogoFileName(slot));
    }

    private static void ensureLegacyCustomLogoMigrated(Context context) {
        File dir = new File(context.getFilesDir(), "smart_cube_logo");
        File legacy = new File(dir, LEGACY_CUSTOM_FILE_NAME);
        File slotOne = new File(dir, getCustomLogoFileName(1));
        if (!legacy.exists() || slotOne.exists()) {
            return;
        }
        // Best-effort migration from the old single custom logo storage.
        // If rename fails, the old file still remains usable via the saved URI.
        legacy.renameTo(slotOne);
    }

    private static String getCustomLogoFileName(int slot) {
        int safeSlot = Math.max(1, Math.min(CUSTOM_LOGO_MAX_COUNT, slot));
        return CUSTOM_FILE_PREFIX + safeSlot + CUSTOM_FILE_SUFFIX;
    }

    private static Bitmap decodeBitmap(ContentResolver resolver, Uri uri, int targetPx) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                throw new FileNotFoundException(uri.toString());
            }
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("invalid image");
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetPx);
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                throw new FileNotFoundException(uri.toString());
            }
            return BitmapFactory.decodeStream(in, null, opts);
        }
    }

    private static int calculateInSampleSize(int width, int height, int target) {
        int sampleSize = 1;
        int largest = Math.max(width, height);
        while (largest / sampleSize > target) {
            sampleSize <<= 1;
        }
        return Math.max(1, sampleSize);
    }

    private static Rect centerCropSquare(int width, int height) {
        if (width == height) {
            return new Rect(0, 0, width, height);
        }
        if (width > height) {
            int left = (width - height) / 2;
            return new Rect(left, 0, left + height, height);
        }
        int top = (height - width) / 2;
        return new Rect(0, top, width, top + width);
    }

    private static Bitmap createBuiltinLogo(String builtinId) {
        if (BUILTIN_HTT.equals(builtinId)) {
            return createHttLogo();
        }
        if (BUILTIN_CUBE.equals(builtinId)) {
            return createCubeLogo();
        }
        return createDctimerAiLogo();
    }

    private static Bitmap normalizeCustomLogo(Bitmap source) {
        if (source == null) {
            return null;
        }
        Bitmap result = createLogoCanvas();
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        Rect srcRect = centerCropSquare(source.getWidth(), source.getHeight());
        RectF dstRect = new RectF(0f, 0f, LOGO_SIZE_PX, LOGO_SIZE_PX);

        Bitmap mask = Bitmap.createBitmap(LOGO_SIZE_PX, LOGO_SIZE_PX, Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(mask);
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.WHITE);
        maskCanvas.drawCircle(LOGO_SIZE_PX / 2f, LOGO_SIZE_PX / 2f, LOGO_SIZE_PX / 2f - 1f, maskPaint);

        canvas.drawBitmap(mask, 0f, 0f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, srcRect, dstRect, paint);
        paint.setXfermode(null);
        mask.recycle();
        return result;
    }

    private static Bitmap createDctimerAiLogo() {
        Bitmap bitmap = createLogoCanvas();
        Canvas canvas = new Canvas(bitmap);

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(PRIMARY_COLOR);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(10f);
        canvas.drawCircle(LOGO_SIZE_PX / 2f, LOGO_SIZE_PX / 2f, 82f, ringPaint);

        Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setColor(SECONDARY_COLOR);
        accentPaint.setStrokeWidth(8f);
        accentPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(74f, 74f, 182f, 74f, accentPaint);

        drawCenteredText(canvas, "DCT", 128f, 96f, 34f, SECONDARY_COLOR, false);
        drawCenteredText(canvas, "AI", 128f, 160f, 92f, PRIMARY_COLOR, true);
        return bitmap;
    }

    private static Bitmap createHttLogo() {
        Bitmap bitmap = createLogoCanvas();
        Canvas canvas = new Canvas(bitmap);

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setColor(SECONDARY_COLOR);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(8f);
        RectF border = new RectF(42f, 54f, 214f, 202f);
        canvas.drawRoundRect(border, 42f, 42f, outlinePaint);

        drawCenteredText(canvas, "HTT", 128f, 145f, 84f, PRIMARY_COLOR, true);

        Paint bottomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bottomPaint.setColor(SECONDARY_COLOR);
        bottomPaint.setStrokeWidth(10f);
        bottomPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(92f, 184f, 164f, 184f, bottomPaint);
        return bitmap;
    }

    private static Bitmap createCubeLogo() {
        Bitmap bitmap = createLogoCanvas();
        Canvas canvas = new Canvas(bitmap);
        Path front = new Path();
        front.moveTo(82f, 92f);
        front.lineTo(128f, 64f);
        front.lineTo(174f, 92f);
        front.lineTo(128f, 120f);
        front.close();

        Path left = new Path();
        left.moveTo(82f, 92f);
        left.lineTo(82f, 154f);
        left.lineTo(128f, 182f);
        left.lineTo(128f, 120f);
        left.close();

        Path right = new Path();
        right.moveTo(174f, 92f);
        right.lineTo(174f, 154f);
        right.lineTo(128f, 182f);
        right.lineTo(128f, 120f);
        right.close();

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(PRIMARY_COLOR);
        canvas.drawPath(front, fillPaint);
        fillPaint.setColor(SECONDARY_COLOR);
        canvas.drawPath(left, fillPaint);
        fillPaint.setColor(ACCENT_COLOR);
        canvas.drawPath(right, fillPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.argb(220, 255, 255, 255));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4f);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(front, strokePaint);
        canvas.drawPath(left, strokePaint);
        canvas.drawPath(right, strokePaint);
        return bitmap;
    }

    private static Bitmap createLogoCanvas() {
        return Bitmap.createBitmap(LOGO_SIZE_PX, LOGO_SIZE_PX, Bitmap.Config.ARGB_8888);
    }

    private static void drawCenteredText(Canvas canvas, String text, float centerX, float baselineY,
                                         float textSize, int color, boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        canvas.drawText(text, centerX, baselineY, paint);
    }
}
