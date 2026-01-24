package de.in.lsp.ui.helper;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogTableModel;
import de.in.lsp.ui.ZebraTableRenderer;

/**
 * Manages the columns of the LogView table, including initialization,
 * visibility
 * toggling, and width calculation.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewColumnManager {

    private final JTable table;
    private final LogTableModel model;
    private final List<LogEntry> entries;
    private final Set<Integer> permanentlyHiddenColumns = new HashSet<>();
    private final TableColumn[] allColumns;

    public LogViewColumnManager(JTable table, LogTableModel model, List<LogEntry> entries) {
        this.table = table;
        this.model = model;
        this.entries = entries;

        // Store all columns initially
        TableColumnModel tcm = table.getColumnModel();
        this.allColumns = new TableColumn[tcm.getColumnCount()];
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            allColumns[i] = tcm.getColumn(i);
        }
    }

    public void analyzeColumns(boolean hasTimestamps) {
        if (entries.isEmpty())
            return;

        // 0: Timestamp
        if (!hasTimestamps)
            permanentlyHiddenColumns.add(0);

        // 1: Level
        boolean hasLevel = entries.stream().anyMatch(e -> isNotEmpty(e.level()));
        if (!hasLevel)
            permanentlyHiddenColumns.add(1);

        // 2: Thread
        boolean hasThread = entries.stream().anyMatch(e -> isNotEmpty(e.thread()));
        if (!hasThread)
            permanentlyHiddenColumns.add(2);

        // 3: Logger
        boolean hasLogger = entries.stream().anyMatch(e -> isNotEmpty(e.loggerName()));
        if (!hasLogger)
            permanentlyHiddenColumns.add(3);

        // 4: IP
        boolean hasIp = entries.stream().anyMatch(e -> isNotEmpty(e.ip()));
        if (!hasIp)
            permanentlyHiddenColumns.add(4);
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty() && !"UNKNOWN".equalsIgnoreCase(value.trim());
    }

    public void setupTableColumns() {
        TableColumnModel tcm = table.getColumnModel();
        FontMetrics fm = table.getFontMetrics(table.getFont());

        // 0: Timestamp - Fixed
        setupColumn(tcm, 0, calculateOptimalWidth(fm, 0, 50), true);

        // 1: Level - Fixed
        setupColumn(tcm, 1, calculateOptimalWidth(fm, 1, 50), true);

        // 2: Thread - Resizable, min 100, pref like timestamp
        int tsWidth = calculateOptimalWidth(fm, 0, 50);
        setupResizableColumn(tcm, 2, tsWidth > 0 ? tsWidth : 120);

        // 3: Logger - Resizable, min 100, pref like timestamp
        setupResizableColumn(tcm, 3, tsWidth > 0 ? tsWidth : 120);

        // 4: IP - Fixed
        setupColumn(tcm, 4, calculateOptimalWidth(fm, 4, 50), true);

        // 5: Message - Resizable (Handled by listener for fill)
        TableColumn colMsg = getColumnByModelIndex(tcm, 5);
        if (colMsg != null) {
            colMsg.setMinWidth(100);
            colMsg.setMaxWidth(Integer.MAX_VALUE);
            colMsg.setPreferredWidth(400);
        }

        // 6: Source - Fixed narrow, hide header
        TableColumn colSource = getColumnByModelIndex(tcm, 6);
        if (colSource != null) {
            if (permanentlyHiddenColumns.contains(6)) {
                tcm.removeColumn(colSource);
            } else {
                setColumnWidth(colSource, 24);
                colSource.setHeaderValue(""); // No header text
            }
        }

        // Apply renderer to all currently visible columns
        ZebraTableRenderer renderer = new ZebraTableRenderer();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setCellRenderer(renderer);
        }
    }

    private void setupColumn(TableColumnModel tcm, int modelIndex, int width, boolean fixed) {
        TableColumn col = getColumnByModelIndex(tcm, modelIndex);
        if (col != null) {
            if (permanentlyHiddenColumns.contains(modelIndex)) {
                tcm.removeColumn(col);
            } else {
                setColumnWidth(col, width);
            }
        }
    }

    private void setupResizableColumn(TableColumnModel tcm, int modelIndex, int prefWidth) {
        TableColumn col = getColumnByModelIndex(tcm, modelIndex);
        if (col != null) {
            if (permanentlyHiddenColumns.contains(modelIndex)) {
                tcm.removeColumn(col);
            } else {
                col.setMinWidth(100);
                col.setPreferredWidth(prefWidth);
                col.setMaxWidth(Integer.MAX_VALUE);
            }
        }
    }

    public TableColumn getColumnByModelIndex(TableColumnModel tcm, int modelIndex) {
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i).getModelIndex() == modelIndex) {
                return tcm.getColumn(i);
            }
        }
        return null; // Might be already removed
    }

    private int calculateOptimalWidth(FontMetrics fm, int modelIndex, int maxEntries) {
        int maxWidth = fm.stringWidth(model.getColumnName(modelIndex)); // Start with header width
        int limit = Math.min(maxEntries, model.getRowCount());
        for (int i = 0; i < limit; i++) {
            Object value = model.getValueAt(i, modelIndex);
            if (value != null) {
                maxWidth = Math.max(maxWidth, fm.stringWidth(value.toString()));
            }
        }
        return maxWidth + 10; // Reduced padding
    }

    private void setColumnWidth(TableColumn col, int width) {
        col.setPreferredWidth(width);
        col.setMinWidth(width);
        col.setMaxWidth(width);
    }

    public void setColumnVisibility(int modelIndex, boolean visible) {
        TableColumnModel tcm = table.getColumnModel();

        // Find if it's already in the model
        int viewIndex = -1;
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i).getModelIndex() == modelIndex) {
                viewIndex = i;
                break;
            }
        }

        if (permanentlyHiddenColumns.contains(modelIndex)) {
            return; // Cannot show permanently hidden columns
        }

        if (visible && viewIndex == -1) {
            // Add column back
            tcm.addColumn(allColumns[modelIndex]);
            // Re-sort view indices to match model indices for consistency
            sortViewColumnsByModelIndex();
            setupTableColumns(); // Ensure width/resizing logic is applied to the added column
        } else if (!visible && viewIndex != -1) {
            // Remove column
            tcm.removeColumn(tcm.getColumn(viewIndex));
        }
    }

    public void sortViewColumnsByModelIndex() {
        TableColumnModel tcm = table.getColumnModel();
        List<TableColumn> currentCols = new ArrayList<>();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            currentCols.add(tcm.getColumn(i));
        }

        currentCols.sort((c1, c2) -> Integer.compare(c1.getModelIndex(), c2.getModelIndex()));

        // Remove all and add back in sorted order
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }
        for (TableColumn col : currentCols) {
            tcm.addColumn(col);
        }
    }

    public void hideColumnPermanently(int modelIndex) {
        permanentlyHiddenColumns.add(modelIndex);
        SwingUtilities.invokeLater(this::setupTableColumns);
    }
}
