package de.in.lsp.parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import de.in.lsp.model.LogEntry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests for the Diagnost Services log format parsing.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class DiagnostServicesLogTest {

    @Test
    public void testDiagnostServicesFormat() throws Exception {
        // Pattern extracted from user sample: 09:00:40.743 [restartedMain] INFO
        // com.pacs.client.ClientServices - ...
        // Note: It ends with a tab and filename. We should ideally ignore the suffix.
        String pattern = "%d{HH:mm:ss.SSS} [%t] %level  %logger - %msg%n";
        PatternBasedLogParser parser = new PatternBasedLogParser(pattern);

        String logLine = "09:00:40.743 [restartedMain] INFO  com.pacs.client.ClientServices - Starting ClientServices using Java 21.0.1 with PID 1 (/app/bin started by pacs in /data)\tdiagnost-services-84b487d8d-7dlbq_diagnost-services.log";

        // This will likely fail canParse because the regex ends with $ and doesn't
        // account for the tab suffix
        // if the pattern doesn't end with %msg or something that catches it.
        // Actually %msg catch (.*) so it might work if %msg is at the end.

        // But wait, the user's sample has TWO spaces after INFO.
        // %level is usually \w+.

        assertTrue(parser.canParse(logLine), "Parser should be able to parse the log line");

        ByteArrayInputStream is = new ByteArrayInputStream(logLine.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is, "test.log");

        assertEquals(1, entries.size());
        LogEntry entry = entries.get(0);
        assertEquals("INFO", entry.level());
        assertNotNull(entry.timestamp());
    }
}
