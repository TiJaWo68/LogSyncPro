package de.in.lsp.manager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.in.lsp.model.LogEntry;
import de.in.lsp.parser.ConfigurableLogParser;
import de.in.lsp.parser.LogFormatConfig;
import de.in.lsp.parser.LogParser;
import de.in.lsp.parser.MultiPatternLogParser;
import de.in.lsp.parser.PatternBasedLogParser;

/**
 * Handles the loading and orchestration of log files. Provides support for archives (.zip, .7z, .gz) and auto-detects the correct parser
 * for each log.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogManager implements ArchiveLogLoader.LogManagerHelper {

	private final List<LogParser> parsers = new ArrayList<>();
	private final ArchiveLogLoader archiveLoader;

	public LogManager() {
		// Quarkus (High priority, specific)
		parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} %level [%logger] (%t) %msg%n"));

		// Default Parser: %d{yyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
		parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"));

		// dicomPACS.log, DicomServer_in.log, PacsDBBrowser.log
		parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] (%logger) - %msg%n"));

		// jDicomCC.log
		parsers.add(new PatternBasedLogParser("%level %d{yyyy-MM-dd HH:mm:ss,SSS} [%t] (%logger) - %msg%n"));

		// starter.log
		parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss} : %msg%n"));

		// sw.wrapper.log
		parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} %level - %msg%n"));

		// debug.log
		parsers.add(new PatternBasedLogParser("[%d{MMdd/HHmmss.SSS}:%level:%logger] %msg%n"));

		// localhost.log
		parsers.add(new PatternBasedLogParser("%d{dd-MMM-yyyy HH:mm:ss.SSS} %level [%t] %logger %msg%n"));

		// postman-portable.log
		parsers.add(new PatternBasedLogParser("%d{EEE, dd MMM yyyy HH:mm:ss z} %level %logger > %msg%n"));

		// mirth.log (as PatternBased)
		parsers.add(new PatternBasedLogParser("%level %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %logger: %msg%n"));

		// diagnost-services (reported issue) 09:00:40.743 [restartedMain] INFO com.pacs.client.ClientServices - ... \tSourceFile
		parsers.add(new PatternBasedLogParser("%d{HH:mm:ss.SSS} [%t] %level  %logger - %msg%n"));

		// server.log (Broad pattern, moved lower)
		parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} %level %t [%logger] %msg%n"));

		// Istio Proxy (Tab separated)
		parsers.add(new ConfigurableLogParser(new LogFormatConfig("Istio Proxy", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\\t.*",
				"^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z)\\t(\\w+)\\t(.*?)(?:\\t([a-zA-Z0-9_.-]+))?$",
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", 1, 2, -1, 4, -1, 3)));

		// server.log.dicomservices (Spring Boot default-ish with ISO8601)
		parsers.add(new ConfigurableLogParser(new LogFormatConfig("server.log.dicomservices",
				"^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z?\\s+.*",
				"^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z?)\\s+(\\w+)\\s+\\d+\\s+---\\s+\\[(.*?)\\]\\s*(?:\\[(.*?)\\]\\s*)?(.*?)\\s+:\\s+(.*)$",
				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 1, 2, 3, 5, -1, 6)));

		// Postgres / Patroni
		parsers.add(new MultiPatternLogParser("Postgres (mixed)",
				Arrays.asList("%d{yyyy-MM-dd HH:mm:ss,SSS} %level: %msg%n", "%d{yyyy-MM-dd HH:mm:ss.SSS} UTC [%t] %level %msg%n")));

		// Access Log (Nginx/Apache)
		parsers.add(new PatternBasedLogParser("%h - - [%d{dd/MMM/yyyy:HH:mm:ss Z}] %msg%n"));

		// Legacy-Default as fallback
		parsers.add(new ConfigurableLogParser(new LogFormatConfig("Legacy-Default", "^\\[.*\\] .*", "^\\[(.*?)\\]\\s+(\\w+)\\s+(.*)$",
				"yyyy-MM-dd HH:mm:ss", 1, 2, -1, -1, -1, 3)));

		// Unpack ArchiveLogLoader which handles recursion We pass 'this::parseStream' to allow ArchiveLogLoader to call back into
		// LogManager for parsing unpacked streams
		this.archiveLoader = new ArchiveLogLoader(this, (is, name) -> {
			try {
				return parseWithAutoDetect(is, name);
			} catch (Exception e) {
				return new ArrayList<>();
			}
		});
	}

	public void addParser(LogParser parser) {
		parsers.add(parser);
	}

	public List<File> scanVisibleFiles(File dir) {
		List<File> files = new ArrayList<>();
		File[] list = dir.listFiles();
		if (list != null) {
			for (File f : list) {
				if (f.isFile() && isSupportedLogFile(f.getName())) {
					files.add(f);
				}
			}
		}
		return files;
	}

	public boolean isSupportedLogFile(String name) {
		String n = name.toLowerCase();
		return n.endsWith(".log") || n.endsWith(".txt") || n.endsWith(".zip") || n.endsWith(".7z") || n.endsWith(".gz");
	}

	public List<LogEntry> loadLog(File file) throws Exception {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".zip")) {
			return archiveLoader.loadFromZip(file);
		} else if (name.endsWith(".7z")) {
			return archiveLoader.loadFrom7z(file);
		} else if (name.endsWith(".gz")) {
			return archiveLoader.loadFromGzip(file);
		} else {
			return loadPlainFile(file);
		}
	}

	private List<LogEntry> loadPlainFile(File file) throws Exception {
		try (InputStream is = new FileInputStream(file)) {
			return parseWithAutoDetect(is, file.getName());
		}
	}

	@Override
	public boolean shouldSkipExtension(String name) {
		String n = name.toLowerCase();
		// Skip obvious binary or non-log extensions
		return n.endsWith(".class") || n.endsWith(".jar") || n.endsWith(".exe") || n.endsWith(".dll") || n.endsWith(".so")
				|| n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".pdf")
				|| n.endsWith(".zip") || n.endsWith(".7z") || n.endsWith(".gz") || n.endsWith(".tar") || n.endsWith(".iso");
	}

	public List<LogEntry> parseStream(InputStream is, String sourceName) throws Exception {
		return parseWithAutoDetect(is, sourceName);
	}

	private List<LogEntry> parseWithAutoDetect(InputStream is, String sourceName) throws Exception {
		// Read the first 128KB to detect the parser (increased from 16KB for large headers)
		int bufferSize = 128 * 1024;
		byte[] buffer = new byte[bufferSize];
		int bytesRead = 0;
		int n;
		// Read up to bufferSize
		while (bytesRead < bufferSize && (n = is.read(buffer, bytesRead, bufferSize - bytesRead)) != -1) {
			bytesRead += n;
		}

		if (bytesRead <= 0)
			return new ArrayList<>();

		// Check for binary content in the read buffer
		if (isBinaryContent(buffer, bytesRead)) {
			// Treat as binary, ignore. But we must NOT close the stream if it's a shielded stream from a zip, the caller handles the stream
			// lifecycle (except valid parsing which consumes it). Actually, parseWithAutoDetect is responsible for the stream provided to
			// it. If we return early, we might leave it open? The caller (loadFromZip) uses CloseShieldInputStream, so closing it inside
			// here is a no-op for the underlying zip stream, but good practice to 'close' the wrapper.
			is.close();
			return new ArrayList<>();
		}

		// Create a buffer for proper reuse
		byte[] head = new byte[bytesRead];
		System.arraycopy(buffer, 0, head, 0, bytesRead);

		String firstLine = new BufferedReader(new StringReader(new String(head, java.nio.charset.StandardCharsets.UTF_8))).readLine();

		List<LogParser> candidates = new ArrayList<>();
		for (LogParser parser : parsers) {
			if (parser.canParse(firstLine)) {
				candidates.add(parser);
			}
		}

		LogParser selectedParser = null;
		if (!candidates.isEmpty()) {
			// Try to find the best parser among candidates
			selectedParser = findBestParser(candidates, head, sourceName);
		}

		if (selectedParser == null) {
			// No candidate matched first line OR candidates produced no entries. Try ALL parsers (handles cases where the file starts with
			// a header).
			selectedParser = findBestParser(parsers, head, sourceName);
		}

		if (selectedParser == null) {
			selectedParser = new de.in.lsp.parser.FallbackLogParser();
		}

		// Reconstruct the full stream: Head + Remaining Original Stream We pass this to the parser. The parser WILL close this stream. This
		// is why we need CloseShieldInputStream in the caller.
		InputStream fullStream = new SequenceInputStream(new ByteArrayInputStream(head), is);
		return selectedParser.parse(fullStream, sourceName);
	}

	private boolean isBinaryContent(byte[] buffer, int length) {
		// Simple heuristic: check for null bytes or excessive non-printable characters We check the first 1024 bytes or length
		int checkLen = Math.min(length, 1024);
		int nullCount = 0;
		int controlCount = 0;

		for (int i = 0; i < checkLen; i++) {
			byte b = buffer[i];
			if (b == 0) {
				nullCount++;
			} else if ((b < 32 && b != 9 && b != 10 && b != 13) || b == 127) {
				// Non-printable, excluding tab, LF, CR
				controlCount++;
			}
		}

		// If we have ANY null bytes, fairly certain it's binary
		if (nullCount > 0)
			return true;

		// If > 30% control characters, likely binary
		if (length > 0 && ((double) controlCount / length) > 0.3)
			return true;

		return false;
	}

	private LogParser findBestParser(List<LogParser> candidates, byte[] head, String sourceName) {
		LogParser bestParser = null;
		int maxTimedEntries = 0;
		int maxTotalEntries = 0;

		for (LogParser parser : candidates) {
			try {
				// Parse the header chunk to count entries
				List<LogEntry> entries = parser.parse(new ByteArrayInputStream(head), sourceName);
				int timedEntries = 0;
				for (LogEntry e : entries) {
					if (e.timestamp() != null) {
						timedEntries++;
					}
				}

				// We prefer the parser that finds the most TIMED entries. If timedEntries are equal, we take the one with more total
				// entries.
				if (timedEntries > maxTimedEntries || (timedEntries == maxTimedEntries && entries.size() > maxTotalEntries)) {
					maxTimedEntries = timedEntries;
					maxTotalEntries = entries.size();
					bestParser = parser;
				}
			} catch (Exception e) {
				// Ignore failure
			}
		}

		// Return best parser if it found at least some TIMED entries. If no parser found timed entries, we prefer null to trigger
		// FallbackLogParser.
		return (maxTimedEntries > 0) ? bestParser : null;
	}

}
