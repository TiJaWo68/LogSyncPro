package de.in.lsp.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.in.lsp.model.LogEntry;

/**
 * A fallback parser that captures every line as a raw message. Used when no other parser can handle the file format.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class FallbackLogParser implements LogParser {

	@Override
	public boolean canParse(String firstLine) {
		return true;
	}

	@Override
	public List<LogEntry> parse(InputStream inputStream, String sourceName) throws Exception {
		List<LogEntry> entries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Create a LogEntry with null/empty fields except for the message Timestamp, Level, Thread, Logger are null/UNKNOWN
				entries.add(new LogEntry(null, "", "", "", "", line, sourceName, line));
			}
		}
		return entries;
	}

	@Override
	public String getFormatName() {
		return "Fallback (Raw Lines)";
	}
}
