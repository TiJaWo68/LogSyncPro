package de.in.lsp.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.in.lsp.model.LogEntry;

/**
 * Test for LogManager log loading capabilities.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
class LogManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void testLoadFromZipMultipleFiles() throws Exception {
		// Create a zip file with multiple log files
		File zipFile = tempDir.resolve("logs.zip").toFile();
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			// First file
			zos.putNextEntry(new ZipEntry("file1.log"));
			zos.write("2023-10-27 10:00:00.000 [main] INFO logger - Message 1\n".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();

			// Second file
			zos.putNextEntry(new ZipEntry("file2.log"));
			zos.write("2023-10-27 10:00:01.000 [main] INFO logger - Message 2\n".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}

		LogManager manager = new LogManager();
		List<LogEntry> entries = manager.loadLog(zipFile);

		// Expecting 2 entries, one from each file
		assertEquals(2, entries.size(), "Should load entries from both files in the zip");
		assertTrue(entries.stream().anyMatch(e -> e.message().equals("Message 1")));
		assertTrue(entries.stream().anyMatch(e -> e.message().equals("Message 2")));
	}

	@Test
	void testLoadFromZipWithBinaryFile() throws Exception {
		// Create a zip file with a log file and a binary file
		File zipFile = tempDir.resolve("mixed.zip").toFile();
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			// Log file
			zos.putNextEntry(new ZipEntry("file1.log"));
			zos.write("2023-10-27 10:00:00.000 [main] INFO logger - Message 1\n".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();

			// Binary file (e.g. just random bytes)
			zos.putNextEntry(new ZipEntry("binary.bin"));
			byte[] binaryData = new byte[100];
			binaryData[0] = 0; // Null byte
			binaryData[1] = 1;
			binaryData[2] = 2; // Some control chars
			zos.write(binaryData);
			zos.closeEntry();
		}

		LogManager manager = new LogManager();
		List<LogEntry> entries = manager.loadLog(zipFile);

		// Should only load the text log file
		assertEquals(1, entries.size(), "Should only load entries from the text log file");
		assertEquals("Message 1", entries.get(0).message());
	}

	@Test
	void testLoadFromNestedZip() throws Exception {
		// Create an inner zip file
		File innerZipFile = tempDir.resolve("inner.zip").toFile();
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(innerZipFile))) {
			zos.putNextEntry(new ZipEntry("inner.log"));
			zos.write("2023-10-27 10:00:02.000 [main] INFO logger - Inner Message\n".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}

		// Create an outer zip file containing the inner zip
		File outerZipFile = tempDir.resolve("outer.zip").toFile();
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outerZipFile))) {
			zos.putNextEntry(new ZipEntry("nested/inner.zip"));
			java.nio.file.Files.copy(innerZipFile.toPath(), zos);
			zos.closeEntry();

			zos.putNextEntry(new ZipEntry("outer.log"));
			zos.write("2023-10-27 10:00:01.000 [main] INFO logger - Outer Message\n".getBytes(StandardCharsets.UTF_8));
			zos.closeEntry();
		}

		LogManager manager = new LogManager();
		List<LogEntry> entries = manager.loadLog(outerZipFile);

		// Expecting 2 entries: 1 from outer.log, 1 from inner.log (inside inner.zip)
		assertEquals(2, entries.size(), "Should load entries from nested archives");
		assertTrue(entries.stream().anyMatch(e -> e.message().equals("Outer Message")));
		assertTrue(entries.stream().anyMatch(e -> e.message().equals("Inner Message")));
	}
}
