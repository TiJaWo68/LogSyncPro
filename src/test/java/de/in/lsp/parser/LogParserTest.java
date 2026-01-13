package de.in.lsp.parser;

import de.in.lsp.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test class for verifying the functionality of LogParser and
 * ConfigurableLogParser.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
class LogParserTest {

        @Test
        void testDefaultParser() throws Exception {
                LogFormatConfig config = new LogFormatConfig(
                                "Default",
                                "^\\[.*\\] .*",
                                "^\\[(.*?)\\]\\s+(\\w+)\\s+(.*)$",
                                "yyyy-MM-dd HH:mm:ss",
                                1, 2, -1, -1, -1, 3);
                ConfigurableLogParser parser = new ConfigurableLogParser(config);

                String logContent = "[2023-10-27 10:00:00] INFO This is a test message\n" +
                                "[2023-10-27 10:05:00] ERROR Something went wrong";

                List<LogEntry> entries = parser.parse(
                                new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                                "test.log");

                assertEquals(2, entries.size());
                assertEquals(LocalDateTime.of(2023, 10, 27, 10, 0, 0), entries.get(0).timestamp());
                assertEquals("INFO", entries.get(0).level());
                assertEquals("This is a test message", entries.get(0).message());
                assertEquals("test.log", entries.get(0).sourceFile());
        }

        @Test
        void testPatternBasedLogParser() throws Exception {
                // Pattern: %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
                PatternBasedLogParser parser = new PatternBasedLogParser(
                                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n");

                String logContent = "2023-10-27 10:00:00.123 [main] INFO  de.in.lsp.Main - App started\n" +
                                "2023-10-27 10:00:01.456 [Thread-1] ERROR de.in.lsp.Service - Database connection failed\n"
                                +
                                "java.sql.SQLException: Access denied for user 'root'@'localhost'";

                List<LogEntry> entries = parser.parse(
                                new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                                "app.log");

                assertEquals(2, entries.size());

                LogEntry info = entries.get(0);
                assertEquals(LocalDateTime.of(2023, 10, 27, 10, 0, 0, 123000000), info.timestamp());
                assertEquals("INFO", info.level());
                assertEquals("main", info.thread());
                assertEquals("de.in.lsp.Main", info.loggerName());
                assertEquals("App started", info.message());

                LogEntry error = entries.get(1);
                assertEquals(LocalDateTime.of(2023, 10, 27, 10, 0, 1, 456000000), error.timestamp());
                assertEquals("ERROR", error.level());
                assertEquals("Thread-1", error.thread());
                assertEquals("de.in.lsp.Service", error.loggerName());
                assertTrue(error.message().contains("Database connection failed"));
                assertTrue(error.message().contains("java.sql.SQLException: Access denied"));
        }
}
