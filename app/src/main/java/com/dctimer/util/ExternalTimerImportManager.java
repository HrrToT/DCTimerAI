package com.dctimer.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dctimer.database.DBHelper;
import com.dctimer.database.SessionManager;
import com.dctimer.model.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExternalTimerImportManager {
    public static final int SOURCE_CSTIMER = 1;
    public static final int SOURCE_TWISTY_TIMER = 2;

    public static final int MODE_NEW_SESSION = 1;
    public static final int MODE_APPEND_TO_SESSION = 2;
    public static final int MODE_REPLACE_SESSION = 3;

    public static final int PUZZLE_333 = 32;

    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile("^session(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final String IMPORT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ExternalTimerImportManager() {
    }

    public static class ImportedSolve {
        public int timeMs;
        public Integer penalty;
        public String scramble = "";
        public String originalDate = "";
        public long sourceTimestamp = -1L;
        public int sourceOrder;
    }

    public static class ImportBatch {
        public int sourceType;
        public String sourceLabel = "";
        public final List<ImportedSolve> solves = new ArrayList<>();
        public int skippedTimeCount;
        public int emptyPenaltyCount;
        public int emptyScrambleCount;
        public int emptyDateCount;
        public int droppedNon333Count;
        public int malformedCount;
    }

    public static class ImportTargetOptions {
        public int mode;
        public String newSessionName = "";
        public int targetSessionId = -1;
    }

    public static class ImportExecutionResult {
        public int targetSessionId = -1;
        public int importedCount;
    }

    public static ImportBatch parseText(int sourceType, String text) throws JSONException {
        ImportBatch batch = new ImportBatch();
        batch.sourceType = sourceType;
        if (sourceType == SOURCE_CSTIMER) {
            parseCsTimerText(text, batch);
        } else if (sourceType == SOURCE_TWISTY_TIMER) {
            parseTwistyTimerText(text, batch);
        } else {
            throw new JSONException("Unsupported import source: " + sourceType);
        }
        sortImportedSolves(batch.solves);
        return batch;
    }

    public static ImportExecutionResult importBatch(Context context, ImportBatch batch, ImportTargetOptions options) {
        DBHelper db = new DBHelper(context);
        SessionManager sessionManager = new SessionManager(context, db);
        ImportExecutionResult result = new ImportExecutionResult();
        int targetSessionId = options.targetSessionId;
        if (options.mode == MODE_NEW_SESSION) {
            sessionManager.addSession(options.newSessionName);
            int sessionIndex = sessionManager.getSessionLength() - 1;
            sessionManager.setPuzzle(sessionIndex, PUZZLE_333);
            Session session = sessionManager.getSession(sessionIndex);
            targetSessionId = session.getId();
        } else if (options.mode == MODE_REPLACE_SESSION) {
            db.clearSession(targetSessionId);
        }

        db.prepareResultInsert(targetSessionId);
        SQLiteDatabase sqliteDb = db.getWritableDatabase();
        sqliteDb.beginTransaction();
        try {
            for (ImportedSolve solve : batch.solves) {
                ContentValues cv = new ContentValues();
                cv.put("rest", solve.timeMs);
                int penalty = solve.penalty == null ? 0 : solve.penalty;
                if (penalty == 2) {
                    cv.put("resp", 0);
                    cv.put("resd", 0);
                } else {
                    cv.put("resp", penalty);
                    cv.put("resd", 1);
                }
                cv.put("scr", emptyToBlank(solve.scramble));
                cv.put("time", emptyToBlank(solve.originalDate));
                db.addResult(targetSessionId, cv);
            }
            sqliteDb.setTransactionSuccessful();
        } finally {
            sqliteDb.endTransaction();
            db.close();
        }
        result.targetSessionId = targetSessionId;
        result.importedCount = batch.solves.size();
        return result;
    }

    public static boolean is333Puzzle(int puzzle) {
        return (puzzle >> 5) == 1;
    }

    private static void parseCsTimerText(String text, ImportBatch batch) throws JSONException {
        batch.sourceLabel = "CSTimer";
        JSONObject root = new JSONObject(stripBom(text));
        JSONObject sessionData = extractSessionData(root);
        List<String> sessionKeys = collectCsTimerSessionKeys(root, sessionData, batch);
        int sourceOrder = 0;
        for (String sessionKey : sessionKeys) {
            JSONArray solves = parseJsonArray(root.opt(sessionKey));
            if (solves == null) continue;
            for (int i = 0; i < solves.length(); i++) {
                JSONArray solve = solves.optJSONArray(i);
                if (solve == null) {
                    batch.malformedCount++;
                    continue;
                }
                if (!isCsTimer333Solve(solve)) {
                    batch.droppedNon333Count++;
                    continue;
                }
                JSONArray xTime = solve.optJSONArray(0);
                int timeMs = parseTimeValue(xTime == null ? null : xTime.opt(1));
                if (timeMs < 0) {
                    batch.skippedTimeCount++;
                    continue;
                }
                ImportedSolve item = new ImportedSolve();
                item.timeMs = timeMs;
                item.penalty = parsePenaltyValue(xTime == null ? null : xTime.opt(0));
                if (item.penalty == null) {
                    batch.emptyPenaltyCount++;
                }
                item.scramble = safeOptString(solve, 1);
                if (isEmpty(item.scramble)) {
                    batch.emptyScrambleCount++;
                }
                long timestamp = parseTimestampValue(solve.opt(3));
                item.sourceTimestamp = timestamp;
                if (timestamp >= 0L) {
                    item.originalDate = formatTimestamp(timestamp);
                } else {
                    batch.emptyDateCount++;
                }
                item.sourceOrder = sourceOrder++;
                batch.solves.add(item);
            }
        }
    }

    private static void parseTwistyTimerText(String text, ImportBatch batch) {
        batch.sourceLabel = "Twisty Timer";
        String[] lines = stripBom(text).replace("\r\n", "\n").replace('\r', '\n').split("\n");
        char delimiter = detectDelimitedFileSeparator(lines);
        int sourceOrder = 0;
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.length() == 0) continue;
            List<String> fields = parseDelimitedLine(line, delimiter);
            if (fields.size() >= 7) {
                parseTwistyExtendedRow(fields, batch, sourceOrder);
                sourceOrder++;
                continue;
            }
            if (fields.size() == 3 || fields.size() == 4) {
                parseTwistySimpleRow(fields, batch, sourceOrder);
                sourceOrder++;
                continue;
            }
            if (fields.size() < 3) {
                batch.malformedCount++;
                continue;
            }
            batch.malformedCount++;
        }
    }

    private static JSONObject extractSessionData(JSONObject root) throws JSONException {
        JSONObject direct = parseJsonObject(root.opt("sessionData"));
        if (direct != null) return direct;
        JSONObject properties = parseJsonObject(root.opt("properties"));
        if (properties != null) {
            JSONObject fromProperties = parseJsonObject(properties.opt("sessionData"));
            if (fromProperties != null) return fromProperties;
        }
        return null;
    }

    private static List<String> collectCsTimerSessionKeys(JSONObject root, JSONObject sessionData, ImportBatch batch) throws JSONException {
        List<String> sessionKeys = new ArrayList<>();
        if (sessionData != null) {
            Iterator<String> iterator = sessionData.keys();
            while (iterator.hasNext()) {
                String dataKey = iterator.next();
                JSONObject meta = parseJsonObject(sessionData.opt(dataKey));
                String sessionKey = toCsTimerRootSessionKey(dataKey);
                if (!hasSessionSolveArray(root, sessionKey)) continue;
                if (is333CsTimerSession(meta, dataKey)) {
                    sessionKeys.add(sessionKey);
                } else {
                    batch.droppedNon333Count++;
                }
            }
        } else {
            Iterator<String> iterator = root.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (SESSION_KEY_PATTERN.matcher(key).matches()) {
                    sessionKeys.add(key);
                }
            }
        }
        Collections.sort(sessionKeys, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return extractSessionIndex(left) - extractSessionIndex(right);
            }
        });
        return sessionKeys;
    }

    private static boolean is333CsTimerSession(JSONObject meta, String sessionDataKey) {
        if (meta != null) {
            JSONObject opt = parseJsonObjectQuietly(meta.opt("opt"));
            String scrType = opt == null ? meta.optString("scrType", "") : opt.optString("scrType", meta.optString("scrType", ""));
            scrType = scrType == null ? "" : scrType.trim().toLowerCase(Locale.US);
            if (scrType.length() > 0) {
                return scrType.startsWith("333");
            }
            String name = meta.optString("name", "");
            if (looksLikeNon333Label(name)) return false;
        }
        return !looksLikeNon333Label(sessionDataKey);
    }

    private static boolean looksLikeNon333Label(String value) {
        if (value == null) return false;
        String normalized = value.toLowerCase(Locale.US);
        return normalized.contains("222") || normalized.contains("2x2")
                || normalized.contains("444") || normalized.contains("4x4")
                || normalized.contains("555") || normalized.contains("5x5")
                || normalized.contains("666") || normalized.contains("6x6")
                || normalized.contains("777") || normalized.contains("7x7")
                || normalized.contains("pyr") || normalized.contains("skewb")
                || normalized.contains("clock") || normalized.contains("sq")
                || normalized.contains("mega");
    }

    private static boolean isCsTimer333Solve(JSONArray solve) {
        JSONArray info = solve.optJSONArray(4);
        if (info == null || info.length() == 0) return true;
        String tag = info.optString(info.length() - 1, "").trim().toLowerCase(Locale.US);
        if (tag.length() == 0) return true;
        return tag.equals("333") || tag.startsWith("333");
    }

    private static boolean hasSessionSolveArray(JSONObject root, String sessionKey) throws JSONException {
        Object value = root.opt(sessionKey);
        if (value == null) return false;
        return parseJsonArray(value) != null;
    }

    private static String toCsTimerRootSessionKey(String dataKey) {
        if (dataKey == null) return "";
        String trimmed = dataKey.trim();
        if (trimmed.startsWith("session")) return trimmed;
        return "session" + trimmed;
    }

    private static int extractSessionIndex(String sessionKey) {
        Matcher matcher = SESSION_KEY_PATTERN.matcher(sessionKey);
        if (!matcher.matches()) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static JSONObject parseJsonObject(Object value) throws JSONException {
        if (value instanceof JSONObject) return (JSONObject) value;
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.length() == 0) return null;
            return new JSONObject(text);
        }
        return null;
    }

    private static JSONObject parseJsonObjectQuietly(Object value) {
        try {
            return parseJsonObject(value);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static JSONArray parseJsonArray(Object value) throws JSONException {
        if (value instanceof JSONArray) return (JSONArray) value;
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.length() == 0) return null;
            return new JSONArray(text);
        }
        return null;
    }

    private static int parseTimeValue(Object value) {
        if (value == null) return -1;
        if (value instanceof Number) {
            long time = Math.round(((Number) value).doubleValue());
            return time > 0 && time <= Integer.MAX_VALUE ? (int) time : -1;
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) return -1;
        if (text.matches("-?\\d+")) {
            try {
                long time = Long.parseLong(text);
                return time > 0 && time <= Integer.MAX_VALUE ? (int) time : -1;
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return StringUtils.parseTime(text.replace(',', '.'));
    }

    private static Integer parsePenaltyValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return mapPenaltyNumber(Math.round(((Number) value).doubleValue()));
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) return null;
        if ("dnf".equalsIgnoreCase(text)) return 2;
        if ("+2".equals(text) || "2+".equalsIgnoreCase(text) || "plus2".equalsIgnoreCase(text)) return 1;
        if (text.matches("-?\\d+")) {
            try {
                return mapPenaltyNumber(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer mapPenaltyNumber(long value) {
        if (value == 0L) return 0;
        if (value == 1L || value == 2000L) return 1;
        if (value == 2L || value == -1L) return 2;
        return null;
    }

    private static long parseTimestampValue(Object value) {
        if (value == null) return -1L;
        if (value instanceof Number) {
            return normalizeTimestamp(Math.round(((Number) value).doubleValue()));
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) return -1L;
        if (text.matches("-?\\d+")) {
            try {
                return normalizeTimestamp(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }
        long isoTime = parseIsoTimestamp(text);
        if (isoTime >= 0L) return isoTime;
        return -1L;
    }

    private static long normalizeTimestamp(long value) {
        if (value <= 0L) return -1L;
        if (value < 100000000000L) {
            value *= 1000L;
        }
        return value;
    }

    private static String safeOptString(JSONArray array, int index) {
        Object value = array.opt(index);
        if (value == null || value == JSONObject.NULL) return "";
        return String.valueOf(value).trim();
    }

    private static char detectDelimitedFileSeparator(String[] lines) {
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.length() == 0) continue;
            if (line.indexOf(';') >= 0) return ';';
            if (line.indexOf(',') >= 0) return ',';
        }
        return ';';
    }

    private static List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private static boolean isTwistyHeaderRow(String firstField) {
        return "Puzzle".equalsIgnoreCase(firstField) || firstField.toLowerCase(Locale.US).contains("puzzle");
    }

    private static boolean isTwisty333Puzzle(String puzzle) {
        if (puzzle == null) return false;
        String normalized = puzzle.toLowerCase(Locale.US).replace(" ", "");
        return normalized.equals("333")
                || normalized.equals("3x3")
                || normalized.equals("3x3x3")
                || normalized.equals("3x3x3cube");
    }

    private static void sortImportedSolves(List<ImportedSolve> solves) {
        Collections.sort(solves, new Comparator<ImportedSolve>() {
            @Override
            public int compare(ImportedSolve left, ImportedSolve right) {
                if (left.sourceTimestamp >= 0L && right.sourceTimestamp >= 0L) {
                    if (left.sourceTimestamp < right.sourceTimestamp) return -1;
                    if (left.sourceTimestamp > right.sourceTimestamp) return 1;
                } else if (left.sourceTimestamp >= 0L) {
                    return -1;
                } else if (right.sourceTimestamp >= 0L) {
                    return 1;
                }
                return left.sourceOrder - right.sourceOrder;
            }
        });
    }

    private static String formatTimestamp(long timestamp) {
        return new SimpleDateFormat(IMPORT_DATE_PATTERN, Locale.getDefault()).format(new Date(timestamp));
    }

    private static long parseIsoTimestamp(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.length() == 0) return -1L;
        if (normalized.matches(".*[+-]\\d\\d:\\d\\d$")) {
            normalized = normalized.substring(0, normalized.length() - 3) + normalized.substring(normalized.length() - 2);
        }
        try {
            return new SimpleDateFormat(ISO_DATE_PATTERN, Locale.US).parse(normalized).getTime();
        } catch (ParseException | NullPointerException ignored) {
            return -1L;
        }
    }

    private static void parseTwistyExtendedRow(List<String> fields, ImportBatch batch, int sourceOrder) {
        String puzzle = fields.get(0).trim();
        if (isTwistyHeaderRow(puzzle)) {
            return;
        }
        if (!isTwisty333Puzzle(puzzle)) {
            batch.droppedNon333Count++;
            return;
        }
        int timeMs = parseTimeValue(fields.get(2));
        if (timeMs < 0) {
            batch.skippedTimeCount++;
            return;
        }
        ImportedSolve item = new ImportedSolve();
        item.timeMs = timeMs;
        item.penalty = parsePenaltyValue(fields.get(5));
        if (item.penalty == null) {
            batch.emptyPenaltyCount++;
        }
        item.scramble = fields.get(4).trim();
        if (isEmpty(item.scramble)) {
            batch.emptyScrambleCount++;
        }
        long timestamp = parseTimestampValue(fields.get(3));
        item.sourceTimestamp = timestamp;
        if (timestamp >= 0L) {
            item.originalDate = formatTimestamp(timestamp);
        } else {
            batch.emptyDateCount++;
        }
        item.sourceOrder = sourceOrder;
        batch.solves.add(item);
    }

    private static void parseTwistySimpleRow(List<String> fields, ImportBatch batch, int sourceOrder) {
        int timeMs = parseTimeValue(fields.get(0));
        if (timeMs < 0) {
            batch.skippedTimeCount++;
            return;
        }
        ImportedSolve item = new ImportedSolve();
        item.timeMs = timeMs;
        if (fields.size() >= 4) {
            item.penalty = parsePenaltyValue(fields.get(3));
            if (item.penalty == null) {
                batch.emptyPenaltyCount++;
            }
        } else {
            item.penalty = null;
            batch.emptyPenaltyCount++;
        }
        item.scramble = fields.get(1).trim();
        if (isEmpty(item.scramble)) {
            batch.emptyScrambleCount++;
        }
        long timestamp = parseTimestampValue(fields.get(2));
        item.sourceTimestamp = timestamp;
        if (timestamp >= 0L) {
            item.originalDate = formatTimestamp(timestamp);
        } else {
            batch.emptyDateCount++;
        }
        item.sourceOrder = sourceOrder;
        batch.solves.add(item);
    }

    private static String stripBom(String text) {
        if (text != null && text.length() > 0 && text.charAt(0) == '\ufeff') {
            return text.substring(1);
        }
        return text == null ? "" : text;
    }

    private static String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }
}
