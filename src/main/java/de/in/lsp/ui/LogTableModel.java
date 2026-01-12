package de.in.lsp.ui;

import de.in.lsp.model.LogEntry;
import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Custom table model for displaying list of LogEntry objects.
 * Maps entry fields to table columns: Timestamp, Level, Message, and Source.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogTableModel extends AbstractTableModel {
    private final List<LogEntry> entries;
    private final String[] columns = { "Timestamp", "Level", "Thread", "Logger", "Message", "Source" };

    public LogTableModel(List<LogEntry> entries) {
        this.entries = entries;
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        LogEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> entry.getFormattedTimestamp();
            case 1 -> entry.level();
            case 2 -> entry.getSimpleThreadName();
            case 3 -> entry.getSimpleLoggerName();
            case 4 -> entry.message();
            case 5 -> entry.sourceFile();
            default -> null;
        };
    }

    public LogEntry getEntry(int rowIndex) {
        return entries.get(rowIndex);
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public int getUniqueSourceCount() {
        return (int) entries.stream()
                .map(LogEntry::sourceFile)
                .distinct()
                .count();
    }
}
