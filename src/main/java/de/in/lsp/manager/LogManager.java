package de.in.lsp.manager;

import de.in.lsp.model.LogEntry;
import de.in.lsp.parser.ConfigurableLogParser;
import de.in.lsp.parser.LogFormatConfig;
import de.in.lsp.parser.LogParser;
import de.in.lsp.parser.PatternBasedLogParser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the loading and orchestration of log files.
 * Provides support for archives (.zip, .7z, .gz) and auto-detects the correct
 * parser for each log.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogManager {

    private final List<LogParser> parsers = new ArrayList<>();

    public LogManager() {
        // Default Parser: %d{yyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n
        parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"));

        // dicomPACS.log, DicomServer_in.log, PacsDBBrowser.log
        parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} [%t] (%logger) - %msg%n"));

        // jDicomCC.log
        parsers.add(new PatternBasedLogParser("%level %d{yyyy-MM-dd HH:mm:ss,SSS} [%t] (%logger) - %msg%n"));

        // starter.log
        parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss} : %msg%n"));

        // sw.wrapper.log
        parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} %level - %msg%n"));

        // debug.log
        parsers.add(new PatternBasedLogParser("[%d{MMdd/HHmmss.SSS}:%level:%logger] %msg%n"));

        // localhost.log
        parsers.add(new PatternBasedLogParser("%d{dd-MMM-yyyy HH:mm:ss.SSS} %level [%t] %logger %msg%n"));

        // postman-portable.log
        parsers.add(new PatternBasedLogParser("%d{EEE, dd MMM yyyy HH:mm:ss z} %level %logger > %msg%n"));

        // server.log
        parsers.add(new PatternBasedLogParser("%d{yyyy-MM-dd HH:mm:ss,SSS} %level %t [%logger] %msg%n"));

        // mirth.log (as PatternBased)
        parsers.add(new PatternBasedLogParser("%level %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %logger: %msg%n"));

        // server.log.dicomservices
        parsers.add(new ConfigurableLogParser(new LogFormatConfig(
                "server.log.dicomservices",
                "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z\\s+.*",
                "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z)\\s+(\\w+)\\s+\\d+\\s+---\\s+\\[.*?\\]\\s+\\[(.*?)\\]\\s+\\[.*?\\]\\s+(.*?)\\s+:\\s+(.*)$",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                1, 2, 3, 4, 5)));

        // Legacy-Default as fallback
        parsers.add(new ConfigurableLogParser(new LogFormatConfig(
                "Legacy-Default",
                "^\\[.*\\] .*",
                "^\\[(.*?)\\]\\s+(\\w+)\\s+(.*)$",
                "yyyy-MM-dd HH:mm:ss",
                1, 2, -1, -1, 3)));
    }

    public void addParser(LogParser parser) {
        parsers.add(parser);
    }

    public List<File> scanVisibleFiles(File dir) {
        List<File> files = new ArrayList<>();
        File[] list = dir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isFile() && isSupportedLogFile(f.getName())) {
                    files.add(f);
                }
            }
        }
        return files;
    }

    public boolean isSupportedLogFile(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".log") || n.endsWith(".txt") || n.endsWith(".zip")
                || n.endsWith(".7z") || n.endsWith(".gz");
    }

    public List<LogEntry> loadLog(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".zip")) {
            return loadFromZip(file);
        } else if (name.endsWith(".7z")) {
            return loadFrom7z(file);
        } else if (name.endsWith(".gz")) {
            return loadFromGzip(file);
        } else {
            return loadPlainFile(file);
        }
    }

    private List<LogEntry> loadPlainFile(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return parseWithAutoDetect(is, file.getName());
        }
    }

    private List<LogEntry> loadFromZip(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return loadFromZipStream(is);
        }
    }

    private List<LogEntry> loadFromZipStream(InputStream is) throws Exception {
        List<LogEntry> allEntries = new ArrayList<>();
        // We must not close the provided InputStream if it's a shielded one from a
        // parent archive,
        // but ZipArchiveInputStream doesn't close the underlying stream when it's
        // closed, usually?
        // Actually, ZipArchiveInputStream.close() DOES close the underlying stream.
        // So we need to wrap the *input* is in a Shield if passing to
        // ZipArchiveInputStream?
        // No, the caller is responsible for shielding if needed.
        // But here we are creating a ZipArchiveInputStream. When we close it
        // (try-with-resources), it closes 'is'.
        // If 'is' is a CloseShieldInputStream from a parent, we are fine.

        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new CloseShieldInputStream(is))) {
            ArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (shouldSkipExtension(entry.getName())) {
                        // Check if it is a nested archive we support
                        if (isSupportedLogFile(entry.getName())) { // isSupportedLogFile checks .zip, .7z, .gz
                            allEntries.addAll(loadNestedEntry(zis, entry.getName()));
                        }
                        continue;
                    }
                    allEntries.addAll(parseWithAutoDetect(new CloseShieldInputStream(zis), entry.getName()));
                }
            }
        }
        return allEntries;
    }

    private List<LogEntry> loadFromGzip(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return loadFromGzipStream(is, file.getName());
        }
    }

    private List<LogEntry> loadFromGzipStream(InputStream is, String name) throws Exception {
        try (InputStream gzipIs = new GzipCompressorInputStream(new CloseShieldInputStream(is))) {
            // Check if specifically this gzip IS an archive or just a compressed file
            // Usually valid usage: file.log.gz -> unpack -> file.log -> parse
            // But if it is file.zip.gz -> unpack -> file.zip -> recursive load
            // The name parameter here is "file.log.gz" or "something.zip.gz"
            String innerName = name;
            if (innerName.toLowerCase().endsWith(".gz")) {
                innerName = innerName.substring(0, innerName.length() - 3);
            }

            if (isSupportedLogFile(innerName)) {
                return loadNestedEntry(gzipIs, innerName);
            } else {
                return parseWithAutoDetect(gzipIs, name);
            }
        }
    }

    private List<LogEntry> loadFrom7z(File file) throws Exception {
        List<LogEntry> allEntries = new ArrayList<>();
        try (SevenZFile sevenZFile = new SevenZFile(file)) {
            org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (shouldSkipExtension(entry.getName())) {
                        if (isSupportedLogFile(entry.getName())) {
                            // Extract to temp file because 7z needs SeekableByteChannel
                            // But wait, we can get an InputStream from 7z entry.
                            // Recursion into Zip from 7z is fine (Zip takes InputStream).
                            // Recursion into 7z from 7z needs temp file.
                            InputStream entryStream = sevenZFile.getInputStream(entry);
                            allEntries.addAll(loadNestedEntry(entryStream, entry.getName()));
                        }
                        continue;
                    }
                    InputStream is = sevenZFile.getInputStream(entry);
                    allEntries.addAll(parseWithAutoDetect(is, entry.getName()));
                }
            }
        }
        return allEntries;
    }

    private List<LogEntry> loadNestedEntry(InputStream is, String name) throws Exception {
        String n = name.toLowerCase();
        if (n.endsWith(".zip")) {
            return loadFromZipStream(is);
        } else if (n.endsWith(".gz")) {
            return loadFromGzipStream(is, name);
        } else if (n.endsWith(".7z")) {
            return loadFrom7zStream(is);
        } else {
            // Treat as plain log file if supported or fallback
            return parseWithAutoDetect(is, name);
        }
    }

    private List<LogEntry> loadFrom7zStream(InputStream is) throws Exception {
        // SevenZFile requires a File or SeekableByteChannel. We must copy to a temp
        // file.
        File tempFile = File.createTempFile("nested-7z", ".7z");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                fos.write(buffer, 0, n);
            }
        }

        List<LogEntry> entries = loadFrom7z(tempFile);
        tempFile.delete(); // Try to delete immediately
        return entries;
    }

    private boolean shouldSkipExtension(String name) {
        String n = name.toLowerCase();
        // Skip obvious binary or non-log extensions
        return n.endsWith(".class") || n.endsWith(".jar") || n.endsWith(".exe")
                || n.endsWith(".dll") || n.endsWith(".so") || n.endsWith(".png")
                || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif")
                || n.endsWith(".pdf") || n.endsWith(".zip") || n.endsWith(".7z")
                || n.endsWith(".gz") || n.endsWith(".tar") || n.endsWith(".iso");
    }

    private List<LogEntry> parseWithAutoDetect(InputStream is, String sourceName) throws Exception {
        // Read the first 16KB to detect the parser
        int bufferSize = 16 * 1024;
        byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        int n;
        // Read up to bufferSize
        while (bytesRead < bufferSize && (n = is.read(buffer, bytesRead, bufferSize - bytesRead)) != -1) {
            bytesRead += n;
        }

        if (bytesRead <= 0)
            return new ArrayList<>();

        // Check for binary content in the read buffer
        if (isBinaryContent(buffer, bytesRead)) {
            // Treat as binary, ignore.
            // But we must NOT close the stream if it's a shielded stream from a zip,
            // the caller handles the stream lifecycle (except valid parsing which consumes
            // it).
            // Actually, parseWithAutoDetect is responsible for the stream provided to it.
            // If we return early, we might leave it open?
            // The caller (loadFromZip) uses CloseShieldInputStream, so closing it inside
            // here is a no-op
            // for the underlying zip stream, but good practice to 'close' the wrapper.
            is.close();
            return new ArrayList<>();
        }

        // Create a buffer for proper reuse
        byte[] head = new byte[bytesRead];
        System.arraycopy(buffer, 0, head, 0, bytesRead);

        String firstLine = new BufferedReader(
                new StringReader(new String(head, java.nio.charset.StandardCharsets.UTF_8))).readLine();

        List<LogParser> candidates = new ArrayList<>();
        for (LogParser parser : parsers) {
            if (parser.canParse(firstLine)) {
                candidates.add(parser);
            }
        }

        LogParser selectedParser = null;
        if (candidates.isEmpty()) {
            // Will fallback
        } else {
            // Try to find the best parser among candidates (even if just 1, we verify it
            // produces data)
            selectedParser = findBestParser(candidates, head, sourceName);
        }

        if (selectedParser == null) {
            selectedParser = new de.in.lsp.parser.FallbackLogParser();
        }

        // Reconstruct the full stream: Head + Remaining Original Stream
        // We pass this to the parser. The parser WILL close this stream.
        // This is why we need CloseShieldInputStream in the caller.
        InputStream fullStream = new SequenceInputStream(new ByteArrayInputStream(head), is);
        return selectedParser.parse(fullStream, sourceName);
    }

    private boolean isBinaryContent(byte[] buffer, int length) {
        // Simple heuristic: check for null bytes or excessive non-printable characters
        // We check the first 1024 bytes or length
        int checkLen = Math.min(length, 1024);
        int nullCount = 0;
        int controlCount = 0;

        for (int i = 0; i < checkLen; i++) {
            byte b = buffer[i];
            if (b == 0) {
                nullCount++;
            } else if ((b < 32 && b != 9 && b != 10 && b != 13) || b == 127) {
                // Non-printable, excluding tab, LF, CR
                controlCount++;
            }
        }

        // If we have ANY null bytes, fairly certain it's binary
        if (nullCount > 0)
            return true;

        // If > 30% control characters, likely binary
        if (length > 0 && ((double) controlCount / length) > 0.3)
            return true;

        return false;
    }

    private LogParser findBestParser(List<LogParser> candidates, byte[] head, String sourceName) {
        LogParser bestParser = null;
        int maxEntries = 0; // Require at least 1 entry

        for (LogParser parser : candidates) {
            try {
                // Parse the header chunk to count entries
                List<LogEntry> entries = parser.parse(new ByteArrayInputStream(head), sourceName);
                if (entries.size() > maxEntries) {
                    maxEntries = entries.size();
                    bestParser = parser;
                }
            } catch (Exception e) {
                // Ignore failure
            }
        }

        return bestParser; // Returns null if no parser produced > 0 entries
    }

    /**
     * Prevents the underlying stream from being closed.
     */
    private static class CloseShieldInputStream extends FilterInputStream {
        public CloseShieldInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            // Do nothing
        }
    }
}
