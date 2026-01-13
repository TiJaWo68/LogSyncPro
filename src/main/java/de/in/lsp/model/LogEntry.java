package de.in.lsp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single log entry with timestamp, level, message, and source
 * information.
 * Supports natural ordering based on the timestamp.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public record LogEntry(
        LocalDateTime timestamp,
        String level,
        String thread,
        String loggerName,
        String ip,
        String message,
        String sourceFile,
        String rawLine) implements Comparable<LogEntry> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(TIME_FORMATTER) : "";
    }

    public String getSimpleLoggerName() {
        if (loggerName == null || loggerName.isEmpty()) {
            return "";
        }

        // Handle cases like "DicomTagPattern.java:105" becoming "DicomTagPattern:105"
        // by removing the standard file extension if it appears before the line number
        // separator
        String clean = loggerName.replace(".java:", ":");

        int lastDot = clean.lastIndexOf('.');
        return lastDot != -1 ? clean.substring(lastDot + 1) : clean;
    }

    public String getSimpleThreadName() {
        if (thread == null || thread.isEmpty()) {
            return "";
        }
        String clean = thread;
        // If it contains (), it's likely a method reference, take only the part before
        // () and then the last segment
        int paren = clean.indexOf('(');
        if (paren != -1) {
            clean = clean.substring(0, paren);
        }
        int lastDot = clean.lastIndexOf('.');
        return lastDot != -1 ? clean.substring(lastDot + 1) : clean;
    }

    public LogEntry appendMessage(String extra) {
        return new LogEntry(timestamp, level, thread, loggerName, ip, message + extra, sourceFile, rawLine + extra);
    }

    @Override
    public int compareTo(LogEntry other) {
        if (this.timestamp == null || other.timestamp == null) {
            return 0;
        }
        return this.timestamp.compareTo(other.timestamp);
    }
}
