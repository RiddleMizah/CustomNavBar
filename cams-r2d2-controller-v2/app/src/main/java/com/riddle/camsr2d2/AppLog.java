package com.riddle.camsr2d2;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

final class AppLog {
    private static final int MAX_LINES = 250;
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private AppLog() {}

    static synchronized void add(String message) {
        lines.addLast(FORMAT.format(new Date()) + "  " + message);
        while (lines.size() > MAX_LINES) lines.removeFirst();
    }

    static synchronized String text() {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) builder.append(line).append('\n');
        return builder.toString();
    }

    static synchronized void clear() {
        lines.clear();
    }
}
