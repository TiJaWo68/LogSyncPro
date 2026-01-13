package de.in.lsp.service;

import de.in.lsp.dto.LogGroup;
import de.in.lsp.manager.LogManager;
import de.in.lsp.model.LogEntry;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Service responsible for loading log files and grouping them by application
 * name.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogFileService {
    private final LogManager logManager = new LogManager();

    public void loadAndMergeByPath(String[] paths, BiConsumer<String, LogGroup> onGroupResult) {
        Map<String, LogGroup> appGroups = new HashMap<>();

        for (String path : paths) {
            File f = new File(path);
            if (f.isDirectory()) {
                for (File child : logManager.scanVisibleFiles(f)) {
                    processFileIntoGroups(child, appGroups);
                }
            } else if (f.isFile()) {
                processFileIntoGroups(f, appGroups);
            }
        }

        appGroups.forEach((appName, group) -> {
            Collections.sort(group.getEntries());
            onGroupResult.accept(appName, group);
        });
    }

    public void processFileIntoGroups(File file, Map<String, LogGroup> groups) {
        try {
            List<LogEntry> entries = logManager.loadLog(file);

            for (LogEntry entry : entries) {
                String sourceName = new File(entry.sourceFile()).getName();
                String appName = detectApplicationName(sourceName);

                LogGroup group = groups.computeIfAbsent(appName, k -> new LogGroup());
                group.addEntry(entry);
                group.addSourceFile(sourceName);
            }
        } catch (Exception e) {
            System.err.println("Error loading " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Group logs by "App Name" (ignores rotation numbers and common suffixes).
     */
    public static String detectApplicationName(String fileName) {
        String name = fileName;
        String prev;

        do {
            prev = name;
            // Remove common end-of-string extensions
            name = name.replaceAll("(?i)\\.(gz|zip|7z|log|txt|bak|old|tmp)$", "");
            // Remove integer suffixes (rotation)
            name = name.replaceAll("\\.\\d+$", "");
            // Remove trailing dates (YYYY-MM-DD, YYYYMMDD)
            name = name.replaceAll("[-_.]?\\d{4}[-_.]?\\d{2}[-_.]?\\d{2}$", "");
            // Remove version-like suffixes
            name = name.replaceAll("[-_.]?(v|V)?\\d+(\\.\\d+)*$", "");
        } while (!name.equals(prev));

        // Final cleanup of trailing separators
        name = name.replaceAll("[-_.]+$", "");

        if (name.isEmpty())
            return fileName;
        return name;
    }

    public void backgroundLoadFiles(List<File> files, Consumer<String> statusUpdate,
            BiConsumer<String, LogGroup> onGroupResult) {
        statusUpdate.accept("Loading files");
        new Thread(() -> {
            try {
                String[] paths = files.stream().map(File::getAbsolutePath).toArray(String[]::new);
                loadAndMergeByPath(paths, onGroupResult);
            } finally {
                statusUpdate.accept("Ready");
            }
        }).start();
    }
}
