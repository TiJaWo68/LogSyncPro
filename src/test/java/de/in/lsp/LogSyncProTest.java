package de.in.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.in.lsp.service.LogFileService;

/**
 * Test for LogSyncPro main entry and common utilities.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
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
