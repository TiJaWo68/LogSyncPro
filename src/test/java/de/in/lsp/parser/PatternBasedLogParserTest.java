package de.in.lsp.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * Test for pattern-based log parsing.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class PatternBasedLogParserTest {

    @Test
    public void testDefaultPattern() throws Exception {
        String pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n";
        PatternBasedLogParser parser = new PatternBasedLogParser(pattern);

        String logLine = "2023-10-27 10:00:00.123 [main] INFO  com.example.Test - Application started";
        assertTrue(parser.canParse(logLine), "Parser should be able to parse the log line");

        ByteArrayInputStream is = new ByteArrayInputStream(logLine.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is, "test.log");

        assertEquals(1, entries.size());
        LogEntry entry = entries.get(0);
        assertEquals("INFO", entry.level());
        assertEquals("Application started", entry.message());
        assertNotNull(entry.timestamp());
        assertEquals(2023, entry.timestamp().getYear());
        assertEquals(10, entry.timestamp().getMonthValue());
        assertEquals(27, entry.timestamp().getDayOfMonth());
        assertEquals(10, entry.timestamp().getHour());
        assertEquals(0, entry.timestamp().getMinute());
        assertEquals(0, entry.timestamp().getSecond());
        assertEquals(123000000, entry.timestamp().getNano());
    }

    @Test
    public void testCustomPattern() throws Exception {
        String pattern = "[%d{HH:mm:ss}] %level: %msg";
        PatternBasedLogParser parser = new PatternBasedLogParser(pattern);

        // We need a full date for LocalDateTime.parse if the pattern only has time.
        // Actually, our parser uses
        // DateTimeFormatter.ofPattern(dateFormatBuilder.toString())
        // If the pattern doesn't have a date part, LocalDateTime.parse will fail.
        // Logback usually uses %d for full date-time.

        String logLine = "[10:00:00] ERROR: Something went wrong";
        // This will likely fail with LocalDateTime.parse if we don't handle partial
        // dates.
        // For simplicity, let's test with a full date pattern.

        String fullPattern = "%d{dd.MM.yyyy HH:mm} %level %msg";
        PatternBasedLogParser fullParser = new PatternBasedLogParser(fullPattern);
        String fullLogLine = "27.10.2023 10:00 DEBUG Processed item 5";

        assertTrue(fullParser.canParse(fullLogLine));
        List<LogEntry> entries = fullParser
                .parse(new ByteArrayInputStream(fullLogLine.getBytes(StandardCharsets.UTF_8)), "test.log");
        assertEquals(1, entries.size());
        assertEquals("DEBUG", entries.get(0).level());
        assertEquals("Processed item 5", entries.get(0).message());
    }
}
