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
import java.time.LocalDate;

/**
 * A LogParser that can be configured using a pattern string similar to
 * Logback/Log4j.
 * Supported placeholders:
 * %d{pattern} - Timestamp
 * %t - Thread name
 * %level (or %-5level) - Log level
 * %logger (or %logger{length}) - Logger name
 * %msg - The actual message
 * %n - Newline (ignored in regex but treated as end of line)
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class PatternBasedLogParser implements LogParser {

    private final String originalPattern;
    private final Pattern regexPattern;
    private final DateTimeFormatter dateTimeFormatter;

    private int timestampGroup = -1;
    private int levelGroup = -1;
    private int threadGroup = -1;
    private int loggerGroup = -1;
    private int ipGroup = -1;
    private int messageGroup = -1;

    public PatternBasedLogParser(String pattern) {
        this.originalPattern = pattern;

        StringBuilder regexBuilder = new StringBuilder();
        StringBuilder dateFormatBuilder = new StringBuilder();

        int groupCount = 1;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '%') {
                i++;
                if (i >= pattern.length())
                    break;
                char next = pattern.charAt(i);
                if (next == 'd') {
                    if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '{') {
                        int end = pattern.indexOf('}', i + 1);
                        if (end > 0) {
                            String df = pattern.substring(i + 2, end);
                            dateFormatBuilder.append(df);
                            regexBuilder.append("(.*?)");
                            timestampGroup = groupCount++;
                            i = end;
                        }
                    }
                } else if (next == 'h') {
                    regexBuilder.append("(.*?)");
                    ipGroup = groupCount++;
                } else if (next == 't') {
                    regexBuilder.append("(.*?)");
                    threadGroup = groupCount++;
                } else if (next == '-' || (next >= '0' && next <= '9')) {
                    // Start of %-5level or %5level
                    int j = i;
                    while (j < pattern.length()
                            && (pattern.charAt(j) == '-' || (pattern.charAt(j) >= '0' && pattern.charAt(j) <= '9'))) {
                        j++;
                    }
                    if (j < pattern.length() && pattern.startsWith("level", j)) {
                        regexBuilder.append("(\\w+)");
                        levelGroup = groupCount++;
                        i = j + 4; // skip 'level'
                    } else if (j < pattern.length() && pattern.startsWith("logger", j)) {
                        regexBuilder.append("(.*?)");
                        loggerGroup = groupCount++;
                        i = j + 5; // skip 'logger'
                        if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '{') {
                            int end = pattern.indexOf('}', i + 1);
                            if (end > 0)
                                i = end;
                        }
                    }
                } else if (next == 'l' && pattern.startsWith("evel", i + 1)) {
                    regexBuilder.append("(\\w+)");
                    levelGroup = groupCount++;
                    i += 4;
                } else if (next == 'm' && pattern.startsWith("sg", i + 1)) {
                    regexBuilder.append("(.*)");
                    messageGroup = groupCount++;
                    i += 2;
                } else if (next == 'n') {
                    // skip
                } else if (next == 'l' && pattern.startsWith("ogger", i + 1)) {
                    regexBuilder.append("(.*?)");
                    loggerGroup = groupCount++;
                    i += 5;
                    if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '{') {
                        int end = pattern.indexOf('}', i + 1);
                        if (end > 0)
                            i = end;
                    }
                } else if (next == 'c' && pattern.startsWith("onfig", i + 1)) {
                    // skip config
                    i += 5;
                } else {
                    if (next == ' ') {
                        // Consolidate multiple spaces in pattern to a single \s+
                        if (regexBuilder.length() >= 3
                                && regexBuilder.substring(regexBuilder.length() - 3).equals("\\s+")) {
                            // already have a space
                        } else {
                            regexBuilder.append("\\s+");
                        }
                    } else {
                        regexBuilder.append(Pattern.quote(String.valueOf(next)));
                    }
                }
            } else {
                if (c == ' ') {
                    if (regexBuilder.length() >= 3
                            && regexBuilder.substring(regexBuilder.length() - 3).equals("\\s+")) {
                        // already have a space
                    } else {
                        regexBuilder.append("\\s+");
                    }
                } else {
                    regexBuilder.append(Pattern.quote(String.valueOf(c)));
                }
            }
        }

        String finalRegex = regexBuilder.toString();
        // System.out.println("Generated Regex: " + finalRegex);

        this.regexPattern = Pattern.compile("^" + finalRegex + "$");

        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatBuilder.toString(), java.util.Locale.US);
    }

    @Override
    public boolean canParse(String firstLine) {
        if (firstLine == null)
            return false;
        return regexPattern.matcher(firstLine).matches();
    }

    @Override
    public List<LogEntry> parse(InputStream inputStream, String sourceName) throws Exception {
        List<LogEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            LogEntry lastEntry = null;
            List<String> headerBuffer = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                // Check if the line matches a NEW entry
                if (canParse(line)) {
                    LogEntry newEntry = parseLine(line, sourceName, null);
                    if (newEntry != null) {
                        // Flush header buffer if first match
                        // If this is the first match, flush header buffer as a single multi-line entry
                        if (lastEntry == null && !headerBuffer.isEmpty()) {
                            String combinedHeader = String.join("\n", headerBuffer);
                            entries.add(new LogEntry(null, "", "", "", "", combinedHeader,
                                    sourceName, combinedHeader));
                            headerBuffer.clear();
                        }
                        entries.add(newEntry);
                        lastEntry = newEntry;
                    } else {
                        // Regex matched but parse failed (e.g. date)
                        if (lastEntry != null) {
                            lastEntry = lastEntry.appendMessage("\n" + line);
                            entries.set(entries.size() - 1, lastEntry);
                        } else {
                            headerBuffer.add(line);
                        }
                    }
                } else if (lastEntry != null) {
                    // Continuation
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
                entries.add(new LogEntry(null, "", "", "", "", combinedHeader, sourceName,
                        combinedHeader));
            }
        }
        return entries;
    }

    /**
     * Parses a single line.
     * If the line matches the pattern, a new LogEntry is returned.
     * If the line does not match but lastEntry is provided, it is appended to
     * lastEntry and the updated entry is returned.
     * Otherwise returns null.
     */
    public LogEntry parseLine(String line, String sourceName, LogEntry lastEntry) {
        Matcher matcher = regexPattern.matcher(line);
        if (matcher.matches()) {
            try {
                LocalDateTime ts = null;
                if (timestampGroup != -1) {
                    String tsStr = matcher.group(timestampGroup).trim();
                    try {
                        ts = LocalDateTime.parse(tsStr, dateTimeFormatter);
                    } catch (Exception e) {
                        // Fallback for time-only formats
                        try {
                            java.time.LocalTime time = java.time.LocalTime.parse(tsStr, dateTimeFormatter);
                            ts = time.atDate(LocalDate.now());
                        } catch (Exception e2) {
                            // If both fail, we can't parse the timestamp.
                            // If this is the start of a match, we might still want to return a
                            // "date-less" entry?
                            // For now, let it fail to parseLine(null) to be treated as header or
                            // continuation.
                            throw e;
                        }
                    }
                }

                String level = (levelGroup != -1) ? matcher.group(levelGroup).trim() : "";
                String thread = (threadGroup != -1) ? matcher.group(threadGroup).trim() : "";
                String logger = (loggerGroup != -1) ? matcher.group(loggerGroup).trim() : "";
                String ip = (ipGroup != -1) ? matcher.group(ipGroup).trim() : "";
                String message = (messageGroup != -1) ? matcher.group(messageGroup) : line;

                return new LogEntry(ts, level, thread, logger, ip, message, sourceName, line);
            } catch (Exception e) {
                // If parsing fails for a line that matches the regex, treat it as multiline if
                // possible
                if (lastEntry != null) {
                    return lastEntry.appendMessage("\n" + line);
                }
                // If we match but can't parse date and have no parent, we still return a
                // "partial" entry?
                // For now, return null to fallback or skip if it's the very first line and
                // broken.
                return null;
            }
        } else if (lastEntry != null) {
            return lastEntry.appendMessage("\n" + line);
        }
        return null;
    }

    @Override
    public String getFormatName() {
        return "Parameterized (" + originalPattern + ")";
    }
}
