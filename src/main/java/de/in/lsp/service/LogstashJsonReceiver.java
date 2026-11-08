package de.in.lsp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.in.lsp.model.LogEntry;
import de.in.lsp.util.LspLogger;

/**
 * Receiver for Logstash JSON log events streamed over TCP as line-delimited JSON (NDJSON). Parses one JSON object per
 * line and maps it to a {@link LogEntry}.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogstashJsonReceiver extends LogStreamServer {

	private static final String DEFAULT_SOURCE_FILE = "moneypoly";

	private final ObjectMapper objectMapper = new ObjectMapper();

	public LogstashJsonReceiver(int port, BiConsumer<LogEntry, SocketAddress> entryConsumer) {
		super(port, "Logstash JSON", entryConsumer);
	}

	@Override
	protected void handleConnection(Socket socket) {
		String clientIp = socket.getInetAddress().getHostAddress();
		SocketAddress remoteAddress = socket.getRemoteSocketAddress();
		int port = socket.getPort();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while (isRunning() && (line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				LogEntry entry = parse(line, clientIp, port);
				if (entry != null) {
					publish(entry, remoteAddress);
				}
			}
		} catch (IOException e) {
			// Connection closed or read error
		} finally {
			publishDisconnect(remoteAddress);
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Parses a single Logstash JSON line into a {@link LogEntry}. Returns {@code null} for lines that cannot be
	 * interpreted as a JSON object.
	 */
	LogEntry parse(String line, String clientIp, int port) {
		try {
			JsonNode node = objectMapper.readTree(line);
			if (node == null || !node.isObject()) {
				return null;
			}

			LocalDateTime timestamp = parseTimestamp(text(node, "@timestamp"));

			String message = Objects.requireNonNullElse(text(node, "message"), "");
			String stackTrace = text(node, "stack_trace");
			if (stackTrace != null && !stackTrace.isEmpty()) {
				message += "\n" + stackTrace;
			}

			return new LogEntry(timestamp, Objects.requireNonNullElse(text(node, "level"), ""),
					Objects.requireNonNullElse(text(node, "thread_name"), ""),
					Objects.requireNonNullElse(text(node, "logger_name"), ""), clientIp, port, message,
					resolveSourceFile(node), line);
		} catch (IOException e) {
			LspLogger.warn("Invalid Logstash JSON line from " + clientIp + ": " + line);
			return null;
		}
	}

	private LocalDateTime parseTimestamp(String text) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(text).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private String resolveSourceFile(JsonNode node) {
		String application = text(node, "application");
		return application == null || application.isEmpty() ? DEFAULT_SOURCE_FILE : application;
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}
}
