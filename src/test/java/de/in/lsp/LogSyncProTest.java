package de.in.lsp;

import de.in.lsp.service.LogFileService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogSyncProTest {

    @Test
    void testDetectApplicationName() {
        // Basic cases
        assertEquals("access", LogFileService.detectApplicationName("access.log"));
        assertEquals("error", LogFileService.detectApplicationName("error.log"));

        // Rotated logs
        assertEquals("access", LogFileService.detectApplicationName("access.log.1"));
        assertEquals("access", LogFileService.detectApplicationName("access.log.2.gz"));

        // Date suffixed
        assertEquals("app", LogFileService.detectApplicationName("app-2023-10-27.log"));
        assertEquals("server", LogFileService.detectApplicationName("server_2023-10-27.log"));

        // Versioned
        assertEquals("my-app", LogFileService.detectApplicationName("my-app-1.0.0.log"));
        assertEquals("service", LogFileService.detectApplicationName("service-v2.log"));

        // Complex
        assertEquals("audit", LogFileService.detectApplicationName("audit.log.2023-10-27.txt"));
    }
}
