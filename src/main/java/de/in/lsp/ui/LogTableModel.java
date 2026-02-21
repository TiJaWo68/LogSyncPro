package de.in.lsp.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.in.lsp.model.LogEntry;

/**
 * Custom table model for displaying list of LogEntry objects. Maps entry fields to table columns: Timestamp, Level, Message, and Source.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogTableModel extends AbstractTableModel {
	private final List<LogEntry> entries;

	public LogTableModel(List<LogEntry> entries) {
		this.entries = entries;
	}

	@Override
	public int getRowCount() {
		return entries.size();
	}

	@Override
	public int getColumnCount() {
		return LogColumn.values().length;
	}

	@Override
	public String getColumnName(int column) {
		return LogColumn.values()[column].getHeader();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		LogEntry entry = entries.get(rowIndex);
		LogColumn col = LogColumn.fromIndex(columnIndex);
		if (col == null)
			return null;
		return switch (col) {
		case TIMESTAMP -> entry.getFormattedTimestamp();
		case LEVEL -> entry.level();
		case THREAD -> entry.getSimpleThreadName();
		case LOGGER -> entry.getSimpleLoggerName();
		case IP -> entry.ip();
		case MESSAGE -> entry.message();
		case SOURCE -> entry.sourceFile();
		};
	}

	public LogEntry getEntry(int rowIndex) {
		return entries.get(rowIndex);
	}

	public List<LogEntry> getEntries() {
		return entries;
	}

	public void addEntry(LogEntry entry) {
		int row = entries.size() - 1;
		fireTableRowsInserted(row, row);
	}

	public int getUniqueSourceCount() {
		return (int) entries.stream().map(LogEntry::sourceFile).distinct().count();
	}
}
