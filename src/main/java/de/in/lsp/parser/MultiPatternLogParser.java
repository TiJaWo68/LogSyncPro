package de.in.lsp.parser;

import de.in.lsp.model.LogEntry;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A LogParser that can handle log files with mixed pattern formats.
 * It tries a list of delegates for each line.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class MultiPatternLogParser implements LogParser {

    private final List<PatternBasedLogParser> delegates;
    private final String name;

    public MultiPatternLogParser(String name, List<String> patterns) {
        this.name = name;
        this.delegates = new ArrayList<>();
        for (String p : patterns) {
            delegates.add(new PatternBasedLogParser(p));
        }
    }

    @Override
    public boolean canParse(String firstLine) {
        if (firstLine == null)
            return false;
        for (PatternBasedLogParser delegate : delegates) {
            if (delegate.canParse(firstLine)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<LogEntry> parse(InputStream inputStream, String sourceName) throws Exception {
        List<LogEntry> allEntries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            LogEntry lastEntry = null;
            List<String> headerBuffer = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                LogEntry newEntry = null;
                for (PatternBasedLogParser delegate : delegates) {
                    // Try to see if it's a new entry
                    if (delegate.canParse(line)) {
                        newEntry = delegate.parseLine(line, sourceName, null);
                        if (newEntry != null)
                            break;
                    }
                }

                if (newEntry != null) {
                    if (lastEntry == null && !headerBuffer.isEmpty()) {
                        String combinedHeader = String.join("\n", headerBuffer);
                        allEntries.add(new LogEntry(null, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", combinedHeader,
                                sourceName, combinedHeader));
                        headerBuffer.clear();
                    }
                    lastEntry = newEntry;
                    allEntries.add(lastEntry);
                } else if (lastEntry != null) {
                    // Continuation
                    lastEntry = lastEntry.appendMessage("\n" + line);
                    allEntries.set(allEntries.size() - 1, lastEntry);
                } else {
                    // Initial header line
                    headerBuffer.add(line);
                }
            }
            if (lastEntry == null && !headerBuffer.isEmpty()) {
                String combinedHeader = String.join("\n", headerBuffer);
                allEntries
                        .add(new LogEntry(null, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", combinedHeader, sourceName,
                                combinedHeader));
            }
        }
        return allEntries;
    }

    @Override
    public String getFormatName() {
        return name;
    }
}
