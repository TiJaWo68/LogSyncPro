package de.in.lsp.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.in.lsp.model.LogEntry;

/**
 * A simple logging utility for internal LogSyncPro events.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LspLogger {

	public enum LogLevel {
		DEBUG(0),
		INFO(1),
		WARN(2),
		ERROR(3);

		final int value;

		LogLevel(int value) {
			this.value = value;
		}

		public boolean isAtLeast(LogLevel other) {
			return this.value >= other.value;
		}
	}

	private static final List<Consumer<LogEntry>> listeners = new ArrayList<>();
	private static final List<LogEntry> history = new ArrayList<>();
	private static LogLevel threshold = LogLevel.INFO;

	public static synchronized void setThreshold(LogLevel level) {
		threshold = level;
	}

	public static synchronized LogLevel getThreshold() {
		return threshold;
	}

	public static synchronized void addListener(Consumer<LogEntry> listener) {
		listeners.add(listener);
		// Playback history to new listeners (like the UI when it's ready)
		for (LogEntry entry : history) {
			listener.accept(entry);
		}
	}

	public static void debug(String message) {
		log(LogLevel.DEBUG, message);
	}

	public static void info(String message) {
		log(LogLevel.INFO, message);
	}

	public static void warn(String message) {
		log(LogLevel.WARN, message);
	}

	public static void error(String message) {
		log(LogLevel.ERROR, message);
	}

	public static void error(String message, Throwable t) {
		log(LogLevel.ERROR, message + (t != null ? ": " + t.getMessage() : ""));
	}

	public static void log(LogLevel level, String message) {
		if (!level.isAtLeast(threshold)) {
			return;
		}

		String caller = StackWalker.getInstance()
				.walk(s -> s
						.filter(f -> !f.getClassName().equals(LspLogger.class.getName()) && !f.getClassName().contains("LogbackAppender"))
						.findFirst().map(StackWalker.StackFrame::getClassName).orElse("LogSyncPro"));

		LogEntry entry = new LogEntry(LocalDateTime.now(), level.name(), Thread.currentThread().getName(), caller, null, message,
				"internal", null);

		synchronized (LspLogger.class) {
			history.add(entry);
			for (Consumer<LogEntry> listener : listeners) {
				listener.accept(entry);
			}
		}
	}
}
