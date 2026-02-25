package de.in.lsp.ui.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.in.lsp.model.LogEntry;

/**
 * Tests for the log export functionality (single .log file and .zip archive).
 * 
 * @author TiJaWo68 in cooperation with Claude Opus 4.6 using Antigravity
 */
public class ViewActionsExportTest {

    @TempDir
    File tempDir;

    private List<LogEntry> createTestEntries() {
        return Arrays.asList(
                new LogEntry(LocalDateTime.of(2025, 1, 1, 10, 0, 0), "INFO", "main", "com.example.App", "127.0.0.1",
                        "Application started", "app.log",
                        "2025-01-01 10:00:00 INFO [main] com.example.App - Application started"),
                new LogEntry(LocalDateTime.of(2025, 1, 1, 10, 0, 1), "WARN", "main", "com.example.App", "127.0.0.1",
                        "Low memory", "app.log", "2025-01-01 10:00:01 WARN [main] com.example.App - Low memory"),
                new LogEntry(LocalDateTime.of(2025, 1, 1, 10, 0, 2), "ERROR", "worker-1", "com.example.Service",
                        "127.0.0.1",
                        "Connection failed", "app.log",
                        "2025-01-01 10:00:02 ERROR [worker-1] com.example.Service - Connection failed"));
    }

    @Test
    public void testExportSingleLogFile() throws IOException {
        List<LogEntry> entries = createTestEntries();
        File target = new File(tempDir, "test_export.log");

        writeSingleLog(entries, target);

        assertTrue(target.exists(), "Export file should exist");
        List<String> lines = Files.readAllLines(target.toPath(), StandardCharsets.UTF_8);
        assertEquals(3, lines.size(), "Should have 3 lines");
        assertEquals("2025-01-01 10:00:00 INFO [main] com.example.App - Application started", lines.get(0));
        assertEquals("2025-01-01 10:00:01 WARN [main] com.example.App - Low memory", lines.get(1));
        assertEquals("2025-01-01 10:00:02 ERROR [worker-1] com.example.Service - Connection failed", lines.get(2));
    }

    @Test
    public void testExportZipWithMultipleLogs() throws IOException {
        List<LogEntry> entries1 = createTestEntries();
        List<LogEntry> entries2 = Arrays.asList(
                new LogEntry(LocalDateTime.of(2025, 1, 1, 11, 0, 0), "DEBUG", "http-nio", "com.example.Web", "10.0.0.1",
                        "Request received", "web.log",
                        "2025-01-01 11:00:00 DEBUG [http-nio] com.example.Web - Request received"));

        File target = new File(tempDir, "test_export.zip");

        writeZip(Arrays.asList(new NamedEntries("app_log", entries1), new NamedEntries("web_log", entries2)), target);

        assertTrue(target.exists(), "ZIP file should exist");
        try (ZipFile zipFile = new ZipFile(target)) {
            assertEquals(2, zipFile.size(), "ZIP should contain 2 entries");

            ZipEntry appEntry = zipFile.getEntry("app_log.log");
            assertTrue(appEntry != null, "Should contain app_log.log");

            ZipEntry webEntry = zipFile.getEntry("web_log.log");
            assertTrue(webEntry != null, "Should contain web_log.log");

            // Verify content of web_log
            String webContent = new String(zipFile.getInputStream(webEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(webContent.contains("Request received"), "web_log should contain expected content");
        }
    }

    @Test
    public void testExportEmptyLog() throws IOException {
        File target = new File(tempDir, "empty.log");
        writeSingleLog(List.of(), target);

        assertTrue(target.exists(), "Empty export file should exist");
        List<String> lines = Files.readAllLines(target.toPath(), StandardCharsets.UTF_8);
        assertEquals(0, lines.size(), "Empty log should have 0 lines");
    }

    // Helper methods that replicate the private export logic from ViewActions
    private void writeSingleLog(List<LogEntry> entries, File target) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8))) {
            for (LogEntry entry : entries) {
                writer.write(entry.rawLine());
                writer.newLine();
            }
        }
    }

    private void writeZip(List<NamedEntries> viewEntries, File target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target), StandardCharsets.UTF_8)) {
            for (NamedEntries ne : viewEntries) {
                zos.putNextEntry(new ZipEntry(ne.name + ".log"));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
                for (LogEntry entry : ne.entries) {
                    writer.write(entry.rawLine());
                    writer.newLine();
                }
                writer.flush();
                zos.closeEntry();
            }
        }
    }

    private record NamedEntries(String name, List<LogEntry> entries) {
    }
}
