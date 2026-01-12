package de.in.lsp;

import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class LogSyncProTest {

    @Test
    void testDetectApplicationName() {
        // Basic cases
        assertEquals("access", LogSyncPro.detectApplicationName("access.log"));
        assertEquals("error", LogSyncPro.detectApplicationName("error.log"));

        // Rotated logs
        assertEquals("access", LogSyncPro.detectApplicationName("access.log.1"));
        assertEquals("access", LogSyncPro.detectApplicationName("access.log.2.gz"));

        // Date suffixed
        assertEquals("app", LogSyncPro.detectApplicationName("app-2023-10-27.log"));
        assertEquals("server", LogSyncPro.detectApplicationName("server_2023-10-27.log"));

        // Versioned
        assertEquals("my-app", LogSyncPro.detectApplicationName("my-app-1.0.0.log"));
        assertEquals("service", LogSyncPro.detectApplicationName("service-v2.log"));

        // Complex
        assertEquals("audit", LogSyncPro.detectApplicationName("audit.log.2023-10-27.txt"));
    }
}
