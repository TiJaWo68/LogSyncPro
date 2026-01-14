package de.in.lsp.parser;

import de.in.lsp.model.LogEntry;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A general-purpose LogParser implementation that uses RegEx-based
 * configuration
 * to extract log entry details like timestamp, level, and message.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ConfigurableLogParser implements LogParser {

    private final LogFormatConfig config;
    private final Pattern pattern;
    private final DateTimeFormatter formatter;

    public ConfigurableLogParser(LogFormatConfig config) {
        this.config = config;
        this.pattern = Pattern.compile(config.entryRegex());
        this.formatter = DateTimeFormatter.ofPattern(config.timestampPattern());
    }

    @Override
    public boolean canParse(String firstLine) {
        if (firstLine == null)
            return false;
        return firstLine.matches(config.firstLinePattern());
    }

    @Override
    public List<LogEntry> parse(InputStream inputStream, String sourceName) throws Exception {
        List<LogEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            LogEntry lastEntry = null;
            List<String> headerBuffer = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        LocalDateTime ts = LocalDateTime.parse(matcher.group(config.timestampGroup()), formatter);
                        String level = matcher.group(config.levelGroup());
                        String thread = (config.threadGroup() != -1) ? matcher.group(config.threadGroup()) : "UNKNOWN";
                        String logger = (config.loggerGroup() != -1) ? matcher.group(config.loggerGroup()) : "UNKNOWN";
                        String ip = (config.ipGroup() != -1) ? matcher.group(config.ipGroup()) : "UNKNOWN";
                        String message = matcher.group(config.messageGroup());

                        // Flush header buffer if first match
                        if (lastEntry == null && !headerBuffer.isEmpty()) {
                            String combinedHeader = String.join("\n", headerBuffer);
                            entries.add(new LogEntry(null, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", combinedHeader,
                                    sourceName, combinedHeader));
                            headerBuffer.clear();
                        }

                        lastEntry = new LogEntry(ts, level, thread, logger, ip, message, sourceName, line);
                        entries.add(lastEntry);
                    } catch (Exception e) {
                        // If parsing fails but it matched the regex, treat it as part of the previous
                        // message
                        if (lastEntry != null) {
                            lastEntry = lastEntry.appendMessage("\n" + line);
                            entries.set(entries.size() - 1, lastEntry);
                        } else {
                            headerBuffer.add(line);
                        }
                    }
                } else if (lastEntry != null) {
                    // Multi-line support: append to the previous entry
                    lastEntry = lastEntry.appendMessage("\n" + line);
                    entries.set(entries.size() - 1, lastEntry);
                } else {
                    // Initial header
                    headerBuffer.add(line);
                }
            }
            // If we NEVER found a match, but have headers, add them as one entry
            if (lastEntry == null && !headerBuffer.isEmpty()) {
                String combinedHeader = String.join("\n", headerBuffer);
                entries.add(new LogEntry(null, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", combinedHeader, sourceName,
                        combinedHeader));
            }
        }
        return entries;
    }

    @Override
    public String getFormatName() {
        return config.name();
    }
}
