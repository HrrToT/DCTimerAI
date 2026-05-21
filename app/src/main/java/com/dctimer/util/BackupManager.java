package com.dctimer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.dctimer.APP;
import com.dctimer.database.DBHelper;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BackupManager {
    private static final String ENTRY_DATABASE = "database.db";
    private static final String ENTRY_SETTINGS = "settings.json";
    private static final String ENTRY_LOGO_DIR = "logos/";
    private static final String ENTRY_BACKGROUND = "background/current_background";
    private static final String PREFS_NAME = "dctimer";
    private static final String LOGO_DIR = "smart_cube_logo";
    private static final String BACKGROUND_DIR = "background_backup";
    private static final String BACKGROUND_FILE_NAME = "restored_background.img";

    public static final int MSG_EXPORT_SUCCESS = 31;
    public static final int MSG_EXPORT_FAIL = 32;
    public static final int MSG_IMPORT_SUCCESS = 33;
    public static final int MSG_IMPORT_FAIL = 34;

    private BackupManager() {
    }

    public static void exportBackup(final Context context, final Uri uri, final Handler handler) {
        new Thread() {
            public void run() {
                try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt");
                     ZipOutputStream zos = new ZipOutputStream(os)) {

                    writeDatabaseEntry(zos);
                    writeSettingsEntry(context, zos);
                    writeBackgroundEntry(context, zos);
                    writeLogoEntries(context, zos);

                    handler.sendEmptyMessage(MSG_EXPORT_SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(MSG_EXPORT_FAIL);
                }
            }
        }.start();
    }

    public static void importBackup(final Context context, final Uri uri, final Handler handler) {
        new Thread() {
            public void run() {
                try (InputStream is = context.getContentResolver().openInputStream(uri);
                     ZipInputStream zis = new ZipInputStream(is)) {

                    File tempDbFile = null;
                    byte[] settingsJsonBytes = null;
                    byte[] backgroundBytes = null;
                    Map<String, byte[]> logoEntries = new HashMap<>();

                    ZipEntry entry;
                    byte[] buffer = new byte[8192];
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (ENTRY_DATABASE.equals(name)) {
                            tempDbFile = readToTempFile(context, zis, "backup_", ".db");
                        } else if (ENTRY_SETTINGS.equals(name)) {
                            settingsJsonBytes = readToByteArray(zis);
                        } else if (ENTRY_BACKGROUND.equals(name)) {
                            backgroundBytes = readToByteArray(zis);
                        } else if (name.startsWith(ENTRY_LOGO_DIR) && !name.equals(ENTRY_LOGO_DIR)) {
                            String fileName = name.substring(ENTRY_LOGO_DIR.length());
                            if (!fileName.isEmpty()) {
                                logoEntries.put(fileName, readToByteArray(zis));
                            }
                        }
                        zis.closeEntry();
                    }

                    boolean dbMerged = false;
                    if (tempDbFile != null) {
                        dbMerged = mergeDatabase(context, tempDbFile);
                        tempDbFile.delete();
                        if (!dbMerged) {
                            throw new IOException("merge database failed");
                        }
                    }

                    if (settingsJsonBytes != null) {
                        mergeSettings(context, settingsJsonBytes);
                    }

                    if (backgroundBytes != null && !restoreBackground(context, backgroundBytes)) {
                        throw new IOException("restore background failed");
                    }

                    mergeLogos(context, logoEntries);

                    handler.sendEmptyMessage(MSG_IMPORT_SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(MSG_IMPORT_FAIL);
                }
            }
        }.start();
    }

    private static void writeDatabaseEntry(ZipOutputStream zos) throws IOException {
        File dbFile = new File(APP.dataPath, "spdcube.db");
        if (!dbFile.exists()) return;
        zos.putNextEntry(new ZipEntry(ENTRY_DATABASE));
        try (FileInputStream fis = new FileInputStream(dbFile)) {
            copyStream(fis, zos);
        }
        zos.closeEntry();
    }

    private static void writeSettingsEntry(Context context, ZipOutputStream zos) throws IOException {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = sp.getAll();
        JSONObject json = new JSONObject(all);
        byte[] data = json.toString().getBytes("UTF-8");
        zos.putNextEntry(new ZipEntry(ENTRY_SETTINGS));
        zos.write(data);
        zos.closeEntry();
    }

    private static void writeBackgroundEntry(Context context, ZipOutputStream zos) throws IOException {
        if (APP.useBgcolor) {
            return;
        }
        try (InputStream is = openCurrentBackgroundInputStream(context)) {
            if (is == null) {
                throw new IOException("background image unavailable");
            }
            zos.putNextEntry(new ZipEntry(ENTRY_BACKGROUND));
            copyStream(is, zos);
            zos.closeEntry();
        }
    }

    private static void writeLogoEntries(Context context, ZipOutputStream zos) throws IOException {
        File dir = new File(context.getFilesDir(), LOGO_DIR);
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) {
                zos.putNextEntry(new ZipEntry(ENTRY_LOGO_DIR + file.getName()));
                try (FileInputStream fis = new FileInputStream(file)) {
                    copyStream(fis, zos);
                }
                zos.closeEntry();
            }
        }
    }

    private static File readToTempFile(Context context, InputStream is, String prefix, String suffix) throws IOException {
        File temp = File.createTempFile(prefix, suffix, context.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            copyStream(is, fos);
        }
        return temp;
    }

    private static byte[] readToByteArray(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        copyStream(is, bos);
        return bos.toByteArray();
    }

    private static boolean mergeDatabase(Context context, File backupDbFile) {
        SQLiteDatabase localDb = null;
        SQLiteDatabase backupDb = null;
        try {
            File localDbFile = new File(APP.dataPath, "spdcube.db");
            if (!localDbFile.exists()) {
                // No local DB, just copy backup over
                try (FileInputStream fis = new FileInputStream(backupDbFile);
                     FileOutputStream fos = new FileOutputStream(localDbFile)) {
                    copyStream(fis, fos);
                }
                return true;
            }

            backupDb = SQLiteDatabase.openDatabase(backupDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            localDb = SQLiteDatabase.openDatabase(localDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);

            // Merge sessions
            Cursor sessionCursor = backupDb.rawQuery("SELECT * FROM sessiontb", null);
            if (sessionCursor != null) {
                while (sessionCursor.moveToNext()) {
                    int id = sessionCursor.getInt(0);
                    Cursor existing = localDb.rawQuery("SELECT COUNT(*) FROM sessiontb WHERE id=?", new String[]{String.valueOf(id)});
                    existing.moveToFirst();
                    if (existing.getInt(0) == 0) {
                        localDb.execSQL("INSERT INTO sessiontb VALUES (?,?,?,?,?,?)",
                                new Object[]{id, sessionCursor.getString(1), sessionCursor.getInt(2),
                                        sessionCursor.getInt(3), sessionCursor.getInt(4), sessionCursor.getInt(5)});
                    }
                    existing.close();
                }
                sessionCursor.close();
            }

            // Merge results for each table
            for (String table : DBHelper.TBL_NAME) {
                Cursor resultCursor = backupDb.rawQuery("SELECT * FROM " + table, null);
                if (resultCursor == null) continue;
                while (resultCursor.moveToNext()) {
                    int resultId = resultCursor.getInt(0);
                    // Check if result already exists
                    Cursor existing = localDb.rawQuery("SELECT COUNT(*) FROM " + table + " WHERE id=?",
                            new String[]{String.valueOf(resultId)});
                    existing.moveToFirst();
                    if (existing.getInt(0) == 0) {
                        if (table.equals(DBHelper.TBL_NAME[15])) {
                            // resultstb has sid column
                            localDb.execSQL("INSERT INTO " + table + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                    buildResultValues(resultCursor, true));
                        } else {
                            localDb.execSQL("INSERT INTO " + table + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                    buildResultValues(resultCursor, false));
                        }
                    }
                    existing.close();
                }
                resultCursor.close();
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (backupDb != null) backupDb.close();
            if (localDb != null) localDb.close();
        }
    }

    private static Object[] buildResultValues(Cursor c, boolean hasSid) {
        int base = hasSid ? 16 : 15;
        Object[] values = new Object[base];
        for (int i = 0; i < base; i++) {
            switch (c.getType(i)) {
                case Cursor.FIELD_TYPE_INTEGER:
                    values[i] = c.getInt(i);
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values[i] = c.getString(i);
                    break;
                case Cursor.FIELD_TYPE_NULL:
                    values[i] = null;
                    break;
                default:
                    values[i] = c.getString(i);
                    break;
            }
        }
        return values;
    }

    private static boolean restoreBackground(Context context, byte[] backgroundBytes) {
        if (backgroundBytes == null || backgroundBytes.length == 0) {
            return true;
        }
        File dir = new File(context.getFilesDir(), BACKGROUND_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }
        File file = new File(dir, BACKGROUND_FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(backgroundBytes);
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putString("picuri", "");
            editor.putString("picpath", file.getAbsolutePath());
            editor.putBoolean("bgcolor", false);
            editor.apply();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void mergeSettings(Context context, byte[] jsonBytes) {
        try {
            String jsonStr = new String(jsonBytes, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                }
            }
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void mergeLogos(Context context, Map<String, byte[]> logoEntries) {
        File dir = new File(context.getFilesDir(), LOGO_DIR);
        if (!dir.exists()) dir.mkdirs();
        for (Map.Entry<String, byte[]> entry : logoEntries.entrySet()) {
            File file = new File(dir, entry.getKey());
            if (file.exists()) continue; // skip existing
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) != -1) {
            os.write(buf, 0, len);
        }
    }

    private static InputStream openCurrentBackgroundInputStream(Context context) throws IOException {
        if (!TextUtils.isEmpty(APP.picUri)) {
            Uri uri = Uri.parse(APP.picUri);
            return context.getContentResolver().openInputStream(uri);
        }
        if (!TextUtils.isEmpty(APP.picPath)) {
            File file = new File(APP.picPath);
            if (file.exists()) {
                return new FileInputStream(file);
            }
        }
        return null;
    }
}
