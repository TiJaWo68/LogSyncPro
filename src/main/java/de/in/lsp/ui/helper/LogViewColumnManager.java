package de.in.lsp.ui.helper;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogColumn;
import de.in.lsp.ui.LogTableModel;
import de.in.lsp.ui.ZebraTableRenderer;

/**
 * Manages the columns of the LogView table, including initialization, visibility toggling, width calculation, and dynamic
 * resizing/collapsing.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewColumnManager {

	public static final int MIN_MESSAGE_WIDTH = 250;

	private final JTable table;
	private final LogTableModel model;
	private final List<LogEntry> entries;
	private final Set<Integer> permanentlyHiddenColumns = new HashSet<>();
	private final Set<Integer> manuallyExpandedColumns = new HashSet<>();
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
			permanentlyHiddenColumns.add(LogColumn.TIMESTAMP.getIndex());

		// 1: Level
		boolean hasLevel = entries.stream().anyMatch(e -> isNotEmpty(e.level()));
		if (!hasLevel)
			permanentlyHiddenColumns.add(LogColumn.LEVEL.getIndex());

		// 2: Thread
		boolean hasThread = entries.stream().anyMatch(e -> isNotEmpty(e.thread()));
		if (!hasThread)
			permanentlyHiddenColumns.add(LogColumn.THREAD.getIndex());

		// 3: Logger
		boolean hasLogger = entries.stream().anyMatch(e -> isNotEmpty(e.loggerName()));
		if (!hasLogger)
			permanentlyHiddenColumns.add(LogColumn.LOGGER.getIndex());

		// 4: IP
		boolean hasIp = entries.stream().anyMatch(e -> isNotEmpty(e.ip()));
		if (!hasIp)
			permanentlyHiddenColumns.add(LogColumn.IP.getIndex());

		// 6: Source (Only for Merged views usually)
		long uniqueSources = entries.stream().map(LogEntry::sourceFile).filter(this::isNotEmpty).distinct().count();
		if (uniqueSources <= 1)
			permanentlyHiddenColumns.add(LogColumn.SOURCE.getIndex());
	}

	private boolean isNotEmpty(String value) {
		return value != null && !value.trim().isEmpty() && !"UNKNOWN".equalsIgnoreCase(value.trim());
	}

	public void setupTableColumns() {
		setupTableColumns(table.getFont());
	}

	public void setupTableColumns(Font font) {
		TableColumnModel tcm = table.getColumnModel();
		FontMetrics fm = table.getFontMetrics(font);

		// 0: Timestamp - Fixed
		setupColumn(tcm, LogColumn.TIMESTAMP.getIndex(), calculateOptimalWidth(fm, LogColumn.TIMESTAMP.getIndex(), 50), true);

		// 1: Level - Fixed
		setupColumn(tcm, LogColumn.LEVEL.getIndex(), calculateOptimalWidth(fm, LogColumn.LEVEL.getIndex(), 50), true);

		// 2: Thread - Resizable, min 100, pref like timestamp
		int tsWidth = calculateOptimalWidth(fm, LogColumn.TIMESTAMP.getIndex(), 50);
		setupResizableColumn(tcm, LogColumn.THREAD.getIndex(), tsWidth > 0 ? tsWidth : 120);

		// 3: Logger - Resizable, min 100, pref like timestamp
		setupResizableColumn(tcm, LogColumn.LOGGER.getIndex(), tsWidth > 0 ? tsWidth : 120);

		// 4: IP - Fixed
		setupColumn(tcm, LogColumn.IP.getIndex(), calculateOptimalWidth(fm, LogColumn.IP.getIndex(), 50), true);

		// 5: Message - Resizable (Handled by listener for fill)
		TableColumn colMsg = getColumnByModelIndex(tcm, LogColumn.MESSAGE.getIndex());
		if (colMsg != null) {
			colMsg.setMinWidth(100);
			colMsg.setMaxWidth(Integer.MAX_VALUE);
			colMsg.setPreferredWidth(400);
		}

		// 6: Source - Fixed narrow, hide header
		TableColumn colSource = getColumnByModelIndex(tcm, LogColumn.SOURCE.getIndex());
		if (colSource != null) {
			if (permanentlyHiddenColumns.contains(LogColumn.SOURCE.getIndex())) {
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
				col.setWidth(prefWidth);
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
		col.setMaxWidth(width);
		col.setMinWidth(width);
		col.setPreferredWidth(width);
		col.setWidth(width);
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

	public void restoreColumnWidth(int modelIndex) {
		TableColumn col = getColumnByModelIndex(table.getColumnModel(), modelIndex);
		if (col == null)
			return;

		FontMetrics fm = table.getFontMetrics(table.getFont());

		if (modelIndex == LogColumn.TIMESTAMP.getIndex() || modelIndex == LogColumn.LEVEL.getIndex()
				|| modelIndex == LogColumn.IP.getIndex()) {
			// Fixed columns: Timestamp, Level, IP
			int w = calculateOptimalWidth(fm, modelIndex, 50);
			setColumnWidth(col, w);
		} else if (modelIndex == LogColumn.THREAD.getIndex() || modelIndex == LogColumn.LOGGER.getIndex()) {
			// Resizable columns: Thread, Logger Use timestamp as base width guide or default to 120
			int tsWidth = calculateOptimalWidth(fm, LogColumn.TIMESTAMP.getIndex(), 50);
			int w = (tsWidth > 0) ? tsWidth : 120;
			// Ensure min width is respected
			col.setMinWidth(100);
			col.setMaxWidth(Integer.MAX_VALUE);
			col.setPreferredWidth(w);
			col.setWidth(w);
		}
	}

	public void hideColumnPermanently(int modelIndex) {
		permanentlyHiddenColumns.add(modelIndex);
		SwingUtilities.invokeLater(this::setupTableColumns);
	}

	public void adjustMessageColumnWidth() {
		Component parent = table.getParent();
		if (!(parent instanceof JViewport))
			return;

		int totalWidth = parent.getWidth();
		if (totalWidth <= 0)
			return;

		TableColumnModel tcm = table.getColumnModel();
		TableColumn msgCol = null;
		List<TableColumn> otherCols = new ArrayList<>();
		int currentOthersWidth = 0;

		for (int i = 0; i < tcm.getColumnCount(); i++) {
			TableColumn col = tcm.getColumn(i);
			if (col.getModelIndex() == LogColumn.MESSAGE.getIndex()) {
				msgCol = col;
			} else {
				otherCols.add(col);
				currentOthersWidth += col.getWidth();
			}
		}

		if (msgCol == null)
			return;

		// Collapsing Candidates & Priority: IP(4), Thread(2), Level(1), Logger(3), Timestamp(0-Last)
		List<Integer> collapsePriority = List.of(LogColumn.IP.getIndex(), LogColumn.THREAD.getIndex(), LogColumn.LEVEL.getIndex(),
				LogColumn.LOGGER.getIndex(), LogColumn.TIMESTAMP.getIndex());
		int availableForMsg = totalWidth - currentOthersWidth;

		if (availableForMsg < MIN_MESSAGE_WIDTH) {
			int needed = MIN_MESSAGE_WIDTH - availableForMsg;

			for (Integer modelIdx : collapsePriority) {
				if (needed <= 0)
					break;

				TableColumn target = null;
				for (TableColumn c : otherCols) {
					if (c.getModelIndex() == modelIdx) {
						target = c;
						break;
					}
				}

				if (target != null && !manuallyExpandedColumns.contains(modelIdx)) {
					// Check if collapsible (currently > 30)
					if (target.getWidth() > 30) {
						// Minimum collapsed width
						int newW = 20;
						int currentW = target.getWidth();
						int saved = currentW - newW;

						if (saved > 0) {
							target.setMinWidth(newW);
							target.setMaxWidth(newW);
							target.setPreferredWidth(newW);
							target.setWidth(newW);
							needed -= saved;
						}
					}
				}
			}
		} else {
			// Check expansion if space is available (Reverse Priority)
			List<Integer> expandPriority = new ArrayList<>(collapsePriority);
			Collections.reverse(expandPriority);

			int surplus = availableForMsg - MIN_MESSAGE_WIDTH;

			for (Integer modelIdx : expandPriority) {
				TableColumn target = null;
				for (TableColumn c : otherCols) {
					if (c.getModelIndex() == modelIdx) {
						target = c;
						break;
					}
				}

				// If collapsed
				if (target != null && target.getWidth() <= 30) {
					// We use a heuristic width to check if we can expand Ideally we would know the exact restore width, but 100 is a safe
					// bet for check
					int estimateRestoreW = 100;

					if (surplus >= estimateRestoreW) {
						restoreColumnWidth(modelIdx);
						int actualRestoreW = target.getWidth();

						surplus -= (actualRestoreW - 20);
						manuallyExpandedColumns.remove(modelIdx);
					}
				}
			}
		}

		// Final Step: Message Column takes remaining space
		int finalOthersWidth = 0;
		for (TableColumn c : otherCols) {
			finalOthersWidth += c.getWidth();
		}

		int newMsgWidth = Math.max(10, totalWidth - finalOthersWidth);
		msgCol.setPreferredWidth(newMsgWidth);
		msgCol.setWidth(newMsgWidth);
	}

	public void onUncollapseColumn(int modelIndex) {
		manuallyExpandedColumns.add(modelIndex);
		restoreColumnWidth(modelIndex);
		// Re-layout (might shrink message)
		adjustMessageColumnWidth();
	}
}
