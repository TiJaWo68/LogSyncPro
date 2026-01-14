package de.in.lsp.util;

import de.in.lsp.model.LogEntry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A simple logging utility for internal LogSyncPro events.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LspLogger {

    private static final List<Consumer<LogEntry>> listeners = new ArrayList<>();
    private static final List<LogEntry> history = new ArrayList<>();

    public static synchronized void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
        // Playback history to new listeners (like the UI when it's ready)
        for (LogEntry entry : history) {
            listener.accept(entry);
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void error(String message, Throwable t) {
        log("ERROR", message + (t != null ? ": " + t.getMessage() : ""));
    }

    private static synchronized void log(String level, String message) {
        LogEntry entry = new LogEntry(
                LocalDateTime.now(),
                level,
                Thread.currentThread().getName(),
                "LogSyncPro",
                null,
                message,
                "internal",
                null);
        history.add(entry);
        for (Consumer<LogEntry> listener : listeners) {
            listener.accept(entry);
        }
    }
}
