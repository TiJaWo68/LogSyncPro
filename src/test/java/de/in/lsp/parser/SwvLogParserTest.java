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
 * Tests for SWV log format parsing. The SWV log contains two distinct formats:
 * Logback internal lines and Spring Boot condensed lines,
 * both with time-only timestamps (no date component).
 *
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class SwvLogParserTest {

    /**
     * Tests parsing of Logback internal format lines like: "10:31:39,416 |-INFO in
     * ch.qos.logback.classic.LoggerContext[default] - This is
     * logback-classic version 1.5.18"
     */
    @Test
    public void testLogbackInternalFormat() throws Exception {
        ConfigurableLogParser parser = new ConfigurableLogParser(new LogFormatConfig("SWV Logback Internal",
                "^\\d{2}:\\d{2}:\\d{2},\\d{3}\\s+\\|-\\w+\\s+in\\s+.*",
                "^(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+\\|-(\\w+)\\s+in\\s+(.*?)\\s+-\\s+(.*)$", "HH:mm:ss,SSS", 1, 2, -1,
                3, -1, 4));

        String logContent = "10:31:39,416 |-INFO in ch.qos.logback.classic.LoggerContext[default] - This is logback-classic version 1.5.18\n"
                + "10:31:39,485 |-INFO in ch.qos.logback.classic.util.ContextInitializer@53142455 - Here is a list of configurators\n"
                + "10:31:45,897 |-WARN in ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull - Missing watchable .xml or .properties files.";

        assertTrue(parser.canParse(
                "10:31:39,416 |-INFO in ch.qos.logback.classic.LoggerContext[default] - This is logback-classic version 1.5.18"));

        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                "swv.log");

        assertEquals(3, entries.size());

        LogEntry first = entries.get(0);
        assertNotNull(first.timestamp());
        assertEquals(10, first.timestamp().getHour());
        assertEquals(31, first.timestamp().getMinute());
        assertEquals(39, first.timestamp().getSecond());
        assertEquals("INFO", first.level());
        assertEquals("ch.qos.logback.classic.LoggerContext[default]", first.loggerName());
        assertEquals("This is logback-classic version 1.5.18", first.message());

        LogEntry warn = entries.get(2);
        assertEquals("WARN", warn.level());
        assertEquals("ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull", warn.loggerName());
    }

    /**
     * Tests parsing of Spring Boot condensed format lines like: "10:31:50.633 INFO
     * at.dedalus.swv.SwvApplication - Starting SwvApplication
     * v1.0.0"
     */
    @Test
    public void testSpringBootCondensedFormat() throws Exception {
        ConfigurableLogParser parser = new ConfigurableLogParser(new LogFormatConfig("SWV Spring Boot",
                "^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\w+\\s+.*",
                "^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+(.*?)\\s+-\\s+(.*)$", "HH:mm:ss.SSS", 1, 2, -1, 3, -1,
                4));

        String logContent = "10:31:50.633 INFO  at.dedalus.swv.SwvApplication - Starting SwvApplication v1.0.0 using Java 21.0.9\n"
                + "10:32:07.737 INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port 443 (http)\n"
                + "10:32:56.170 ERROR a.d.swv.util.ExportXmlServiceImpl - Config-tool command failed with exit code: 2. Output:\n"
                + "Connection failedjava.lang.NullPointerException: Cannot invoke \"String.indexOf(String)\"\n"
                + "10:33:10.823 INFO  at.dedalus.swv.SwvApplication - Started SwvApplication in 88.933 seconds";

        assertTrue(parser
                .canParse(
                        "10:31:50.633 INFO  at.dedalus.swv.SwvApplication - Starting SwvApplication v1.0.0 using Java 21.0.9"));

        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                "swv.log");

        assertEquals(4, entries.size());

        LogEntry first = entries.get(0);
        assertNotNull(first.timestamp());
        assertEquals(10, first.timestamp().getHour());
        assertEquals(31, first.timestamp().getMinute());
        assertEquals(50, first.timestamp().getSecond());
        assertEquals(633000000, first.timestamp().getNano());
        assertEquals("INFO", first.level());
        assertEquals("at.dedalus.swv.SwvApplication", first.loggerName());
        assertEquals("Starting SwvApplication v1.0.0 using Java 21.0.9", first.message());

        // Error entry should include multi-line continuation
        LogEntry error = entries.get(2);
        assertEquals("ERROR", error.level());
        assertTrue(error.message().contains("Config-tool command failed"));
        assertTrue(error.message().contains("NullPointerException"));

        LogEntry last = entries.get(3);
        assertEquals("INFO", last.level());
        assertTrue(last.message().contains("Started SwvApplication"));
    }

    /**
     * Tests that header lines (like ASCII art banner) before the first parseable
     * entry are collected as a single header entry.
     */
    @Test
    public void testHeaderLinesBeforeFirstEntry() throws Exception {
        ConfigurableLogParser parser = new ConfigurableLogParser(new LogFormatConfig("SWV Spring Boot",
                "^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\w+\\s+.*",
                "^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+(.*?)\\s+-\\s+(.*)$", "HH:mm:ss.SSS", 1, 2, -1, 3, -1,
                4));

        String logContent = ":: Springboot Version ::                (v3.4.4)\n" + " \n"
                + "10:31:50.633 INFO  at.dedalus.swv.SwvApplication - Starting SwvApplication v1.0.0";

        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                "swv.log");

        assertEquals(2, entries.size());

        // First entry is the header (no timestamp)
        LogEntry header = entries.get(0);
        assertEquals(null, header.timestamp());
        assertTrue(header.message().contains("Springboot Version"));

        // Second entry is the actual log entry
        LogEntry logEntry = entries.get(1);
        assertNotNull(logEntry.timestamp());
        assertEquals("INFO", logEntry.level());
    }

    /**
     * Tests the time-only fallback in ConfigurableLogParser with a full-date config
     * to ensure it still works.
     */
    @Test
    public void testFullDateConfigStillWorks() throws Exception {
        ConfigurableLogParser parser = new ConfigurableLogParser(new LogFormatConfig("Legacy-Default", "^\\[.*\\] .*",
                "^\\[(.*?)\\]\\s+(\\w+)\\s+(.*)$", "yyyy-MM-dd HH:mm:ss", 1, 2, -1, -1, -1, 3));

        String logContent = "[2023-10-27 10:00:00] INFO This is a test message";

        List<LogEntry> entries = parser.parse(new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8)),
                "test.log");

        assertEquals(1, entries.size());
        assertNotNull(entries.get(0).timestamp());
        assertEquals(2023, entries.get(0).timestamp().getYear());
        assertEquals("INFO", entries.get(0).level());
    }
}
