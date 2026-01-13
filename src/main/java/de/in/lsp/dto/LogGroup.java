package de.in.lsp.dto;

import de.in.lsp.model.LogEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Transfer Object representing a group of log entries belonging to the
 * same application.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogGroup {
    private final List<LogEntry> entries = new ArrayList<>();
    private final Set<String> sourceFiles = new HashSet<>();

    public List<LogEntry> getEntries() {
        return entries;
    }

    public Set<String> getSourceFiles() {
        return sourceFiles;
    }

    public int getFileCount() {
        return sourceFiles.size();
    }

    public void addEntry(LogEntry entry) {
        entries.add(entry);
    }

    public void addSourceFile(String sourceFile) {
        sourceFiles.add(sourceFile);
    }
}
