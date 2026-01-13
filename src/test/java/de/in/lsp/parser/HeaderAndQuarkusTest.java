package de.in.lsp.parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import de.in.lsp.model.LogEntry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HeaderAndQuarkusTest {

    @Test
    public void testQuarkusLogWithHeader() throws Exception {
        String pattern = "%d{yyyy-MM-dd HH:mm:ss,SSS} %level [%logger] (%t) %msg%n";
        PatternBasedLogParser parser = new PatternBasedLogParser(pattern);

        String logContent = "Installing Certs...\n" +
                "Installing /certificates/customer-custom-cert.crt\n" +
                "Certificate was added to keystore\n" +
                "Installing /certificates/other-cert-1.crt\n" +
                "Certificate was added to keystore\n" +
                "__  ____  __  _____   ___  __ ____  ______ \n" +
                " --/ __ \\/ / / / _ | / _ \\/ //_/ / / / __/ \n" +
                " -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\\ \\   \n" +
                "--\\___\\_\\____/_/ |_/_/|_/_/|_|\\____/___/   \n" +
                "2026-01-06 11:02:35,715 INFO  [io.qua.sch.run.SimpleScheduler] (main) No scheduled business methods found\n"
                +
                "2026-01-06 11:02:35,761 INFO  [io.quarkus] (main) PACSGate c44ae73 on JVM started in 2.613s.\n" +
                "  with continuation line";

        ByteArrayInputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is, "quarkus.log");

        // Header: 1 entry (combined from 6 lines)
        // Log entries: 2
        assertEquals(3, entries.size(), "Should have 1 header entry (combined) + 2 log entries");

        // Check header entry
        LogEntry header = entries.get(0);
        assertNull(header.timestamp(), "Header entry should have null timestamp");
        assertEquals("UNKNOWN", header.level());
        assertTrue(header.message().contains("Installing Certs..."));
        assertTrue(header.message().contains("Installing /certificates/other-cert-1.crt"));

        // Check log entries
        LogEntry e7 = entries.get(1);
        assertNotNull(e7.timestamp());
        assertEquals("INFO", e7.level());
        assertEquals("io.qua.sch.run.SimpleScheduler", e7.loggerName());
        assertEquals("main", e7.thread());
        assertTrue(e7.message().contains("No scheduled business methods found"));

        LogEntry e8 = entries.get(2);
        assertEquals("INFO", e8.level());
        assertTrue(e8.message().contains("PACSGate c44ae73"));
        assertTrue(e8.message().contains("continuation line"));
    }

    @Test
    public void testLogManagerDetection() throws Exception {
        de.in.lsp.manager.LogManager manager = new de.in.lsp.manager.LogManager();

        String logContent = "Banner Line 1\n" +
                "Banner Line 2\n" +
                "2026-01-06 11:02:35,715 INFO  [io.quarkus] (main) Started\n";

        ByteArrayInputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = manager.parseStream(is, "pacsgate.log");

        // Should detect Quarkus parser and find 2 entries (1 combined header + 1 log)
        assertEquals(2, entries.size());
        assertNotNull(entries.get(1).timestamp(), "Last entry should have a timestamp");
        assertEquals("io.quarkus", entries.get(1).loggerName());
    }
}
