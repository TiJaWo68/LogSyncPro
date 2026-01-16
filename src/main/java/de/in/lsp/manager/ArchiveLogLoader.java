package de.in.lsp.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import de.in.lsp.model.LogEntry;

/**
 * Handles loading logs from various archive formats. Only deals with unpacking
 * and recursion.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ArchiveLogLoader {

    private final LogManagerHelper helper;
    private final BiFunction<InputStream, String, List<LogEntry>> parser;

    public interface LogManagerHelper {
        boolean isSupportedLogFile(String name);

        boolean shouldSkipExtension(String name);
    }

    public ArchiveLogLoader(LogManagerHelper helper, BiFunction<InputStream, String, List<LogEntry>> parser) {
        this.helper = helper;
        this.parser = parser;
    }

    public List<LogEntry> loadFromZip(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return loadFromZipStream(is);
        }
    }

    public List<LogEntry> loadFromZipStream(InputStream is) throws Exception {
        List<LogEntry> allEntries = new ArrayList<>();
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new CloseShieldInputStream(is))) {
            ArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (helper.shouldSkipExtension(entry.getName())) {
                        if (helper.isSupportedLogFile(entry.getName())) {
                            allEntries.addAll(loadNestedEntry(zis, entry.getName()));
                        }
                        continue;
                    }
                    // Use CloseShieldInputStream to prevent parser from closing the Zip stream
                    allEntries.addAll(parser.apply(new CloseShieldInputStream(zis), entry.getName()));
                }
            }
        }
        return allEntries;
    }

    public List<LogEntry> loadFromGzip(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return loadFromGzipStream(is, file.getName());
        }
    }

    public List<LogEntry> loadFromGzipStream(InputStream is, String name) throws Exception {
        try (InputStream gzipIs = new GzipCompressorInputStream(new CloseShieldInputStream(is))) {
            String innerName = name;
            if (innerName.toLowerCase().endsWith(".gz")) {
                innerName = innerName.substring(0, innerName.length() - 3);
            }

            if (helper.isSupportedLogFile(innerName)) {
                return loadNestedEntry(gzipIs, innerName);
            } else {
                return parser.apply(gzipIs, name);
            }
        }
    }

    public List<LogEntry> loadFrom7z(File file) throws Exception {
        List<LogEntry> allEntries = new ArrayList<>();
        try (SevenZFile sevenZFile = new SevenZFile.Builder().setFile(file).get()) {
            org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (helper.shouldSkipExtension(entry.getName())) {
                        if (helper.isSupportedLogFile(entry.getName())) {
                            InputStream entryStream = sevenZFile.getInputStream(entry);
                            allEntries.addAll(loadNestedEntry(entryStream, entry.getName()));
                        }
                        continue;
                    }
                    InputStream is = sevenZFile.getInputStream(entry);
                    allEntries.addAll(parser.apply(is, entry.getName()));
                }
            }
        }
        return allEntries;
    }

    public List<LogEntry> loadNestedEntry(InputStream is, String name) throws Exception {
        String n = name.toLowerCase();
        if (n.endsWith(".zip")) {
            return loadFromZipStream(is);
        } else if (n.endsWith(".gz")) {
            return loadFromGzipStream(is, name);
        } else if (n.endsWith(".7z")) {
            return loadFrom7zStream(is);
        } else {
            return parser.apply(is, name);
        }
    }

    public List<LogEntry> loadFrom7zStream(InputStream is) throws Exception {
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
        tempFile.delete();
        return entries;
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
