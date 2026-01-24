package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import de.in.lsp.model.LogEntry;
/**
 * A self-contained UI component that displays log entries in a table with
 * filtering capabilities.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
import de.in.lsp.ui.helper.LogViewColumnManager;

public class LogView extends JInternalFrame {

	private final LogTableModel model;
	private final JTable table;
	private final LogViewColumnManager columnManager;
	private final TableRowSorter<LogTableModel> sorter;
	private final List<LogEntry> entries;
	private final BiConsumer<LogView, LocalDateTime> onSelectionChanged;
	private final LogViewListener listener;
	private LogDetailView detailView;
	private JDialog detachedDialog;
	private JSplitPane splitPane;
	private FilteredTablePanel filteredTablePanel;
	private boolean maximized = false;
	private String appName;
	private String clientIp;
	private String initialLoggerName;
	private ViewType viewType;

	private LogViewFilterPanel filterPanel;

	private boolean isSelectedForAction = false; // Internal selection state
	private int currentFontSize = 12;

	public LogView(List<LogEntry> entries, String title, BiConsumer<LogView, LocalDateTime> onSelectionChanged,
			LogViewListener listener,
			ViewType viewType) {
		super(title, true, true, true, true);
		this.entries = entries;
		this.onSelectionChanged = onSelectionChanged;
		this.listener = listener;
		this.viewType = viewType;
		this.model = new LogTableModel(entries);
		this.table = new JTable(model);
		this.columnManager = new LogViewColumnManager(table, model, entries);
		this.sorter = new TableRowSorter<>(model);
		for (int i = 0; i < model.getColumnCount(); i++) {
			sorter.setSortable(i, false);
		}
		table.setRowSorter(sorter); // CRITICAL: Link sorter to table

		columnManager.analyzeColumns(hasTimestamps());

		setupUI();

		setupMouseListener();

		// Set initial frame icon
		setFrameIcon(viewType.getIcon());

		// Use InternalFrameListener instead of custom listeners where possible
		addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
			@Override
			public void internalFrameActivated(javax.swing.event.InternalFrameEvent e) {
				listener.onFocusGained(LogView.this);
			}

			@Override
			public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
				if (detachedDialog != null) {
					detachedDialog.dispose();
				}
				listener.onClose(LogView.this);
			}

			@Override
			public void internalFrameIconified(javax.swing.event.InternalFrameEvent e) {
				listener.onMinimize(LogView.this);
			}

			@Override
			public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent e) {
				listener.onMaximize(LogView.this);
			}
		});

		// Setup Title Bar Selection Listener
		setupTitleBarListener();
	}

	private void setupUI() {
		setLayout(new BorderLayout());
		setBorder(null);

		createDetailView();

		// Filter + Table Setup
		columnManager.setupTableColumns();

		// Explicitly set renderer for all columns
		ZebraTableRenderer renderer = new ZebraTableRenderer();
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer(renderer);
		}

		filterPanel = new LogViewFilterPanel(table, sorter, entries);
		filteredTablePanel = new FilteredTablePanel(table, filterPanel);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filteredTablePanel, null);
		splitPane.setResizeWeight(1.0);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);

		add(splitPane, BorderLayout.CENTER);

		filteredTablePanel.getTableScrollPane().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				adjustMessageColumnWidth();
			}
		});

		filterPanel.updateFilters();
		filterPanel.updateAlignment();

		setupKeyBindings();
	}

	private void adjustMessageColumnWidth() {
		TableColumnModel tcm = table.getColumnModel();
		int totalWidth = filteredTablePanel.getTableScrollPane().getViewport().getWidth();
		if (totalWidth <= 0)
			return;

		int otherColsWidth = 0;
		TableColumn msgCol = null;
		for (int i = 0; i < tcm.getColumnCount(); i++) {
			TableColumn col = tcm.getColumn(i);
			if (col.getModelIndex() == 5) {
				msgCol = col;
			} else {
				otherColsWidth += col.getWidth();
			}
		}

		if (msgCol != null) {
			int newWidth = Math.max(100, totalWidth - otherColsWidth);
			msgCol.setPreferredWidth(newWidth);
		}
	}

	private void setupKeyBindings() {
		InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = table.getActionMap();

		// Escape to close detail view
		inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closeDetail");
		actionMap.put("closeDetail", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				toggleDetailView(false);
			}
		});

		// Message filter toggle shortcut
		inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK),
				"toggleFilter");
		actionMap.put("toggleFilter", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (filterPanel != null) {
					filterPanel.getMessageFilterField().setVisible(true);
					filterPanel.updateAlignment();
					filterPanel.getMessageFilterField().requestFocusInWindow();
				}
			}
		});
	}

	private void createDetailView() {
		detailView = new LogDetailView(
				() -> detachDetailView(),
				() -> toggleDetailView(false));
	}

	private void detachDetailView() {
		// Calculate better width:
		// Not excessively wide, but enough for text.
		// Start with 800 or half screen width?
		int width = 800;
		int height = 600;

		java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
		if (gc != null) {
			java.awt.Rectangle bounds = gc.getBounds();
			width = Math.min(bounds.width - 100, 1000);
			height = Math.min(bounds.height - 100, 600);
		}

		if (splitPane.getBottomComponent() == detailView) {
			splitPane.setBottomComponent(null);
		}

		if (detachedDialog == null) {
			detachedDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Log Details",
					java.awt.Dialog.ModalityType.MODELESS);
			detachedDialog.setLayout(new BorderLayout());
			detachedDialog.add(detailView);
			detachedDialog.setSize(width, height);
			detachedDialog.setLocationRelativeTo(this);
			detachedDialog.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent e) {
					reattachDetailView();
				}
			});

			// Add Zoom Shortcuts to Dialog
			javax.swing.JRootPane rootPane = detachedDialog.getRootPane();
			javax.swing.InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			javax.swing.ActionMap actionMap = rootPane.getActionMap();

			inputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "zoomIn");
			inputMap.put(KeyStroke.getKeyStroke("control ADD"), "zoomIn");
			inputMap.put(KeyStroke.getKeyStroke("control PLUS"), "zoomIn");
			actionMap.put("zoomIn", new AbstractAction() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					listener.onIncreaseFontSize();
				}
			});

			inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "zoomOut");
			inputMap.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
			actionMap.put("zoomOut", new AbstractAction() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					listener.onDecreaseFontSize();
				}
			});

		} else {
			detachedDialog.getContentPane().add(detailView);
		}

		detachedDialog.setVisible(true);
	}

	private void reattachDetailView() {
		if (detachedDialog != null) {
			detachedDialog.dispose();
			detachedDialog = null;
		}
		toggleDetailView(true);
	}

	private void toggleDetailView(boolean show) {
		if (show) {
			if (detachedDialog != null && detachedDialog.isVisible()) {
				// Already detached and visible, update content is handled by caller
				return;
			}
			if (splitPane.getBottomComponent() == null) {
				splitPane.setBottomComponent(detailView);
				splitPane.setDividerLocation(0.7);
			}
		} else {
			if (detachedDialog != null) {
				detachedDialog.dispose();
				detachedDialog = null;
			}
			if (splitPane.getBottomComponent() != null) {
				splitPane.setBottomComponent(null);
			}
		}
	}

	public void setColumnVisibility(int modelIndex, boolean visible) {
		columnManager.setColumnVisibility(modelIndex, visible);
	}

	private void setupMouseListener() {
		attachFocusTrigger(table);

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					int row = table.rowAtPoint(e.getPoint());
					int col = table.columnAtPoint(e.getPoint());

					if (row != -1 && col != -1) {
						int modelColumn = table.convertColumnIndexToModel(col);
						if (modelColumn == 0) { // Timestamp column
							LogEntry selected = model.getEntry(table.convertRowIndexToModel(row));
							if (onSelectionChanged != null) {
								onSelectionChanged.accept(LogView.this, selected.timestamp());
							}
						} else if (modelColumn == 5) { // Message column
							LogEntry selected = model.getEntry(table.convertRowIndexToModel(row));
							toggleDetailView(true);
							detailView.setEntry(selected);
						}
					}
				}
			}
		});

		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && splitPane.getBottomComponent() != null) {
				int row = table.getSelectedRow();
				if (row != -1) {
					LogEntry selected = model.getEntry(table.convertRowIndexToModel(row));
					detailView.setEntry(selected);
				}
			}
		});
	}

	public void scrollToTimestamp(LocalDateTime timestamp) {
		// Binary Search for nearest timestamp
		int index = Collections.binarySearch(entries,
				new LogEntry(timestamp, null, null, null, null, null, null, null));
		if (index < 0) {
			index = -(index + 1);
		}
		if (index >= entries.size())
			index = entries.size() - 1;
		if (index < 0)
			index = 0;

		final int finalIndex = index;
		SwingUtilities.invokeLater(() -> {
			int viewRow = table.convertRowIndexToView(finalIndex);
			table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
			table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
		});
	}

	public boolean isMaximized() {
		return maximized;
	}

	public List<LogEntry> getEntries() {
		return model.getEntries();
	}

	private void setupTitleBarListener() {
		SwingUtilities.invokeLater(() -> {
			if (getUI() instanceof javax.swing.plaf.basic.BasicInternalFrameUI ui) {
				JComponent titlePane = ui.getNorthPane();
				if (titlePane != null) {
					titlePane.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							if (SwingUtilities.isRightMouseButton(e)) {
								setViewSelected(!isViewSelected());
							}
						}
					});
				}
			}
		});
	}

	public boolean isViewSelected() {
		return isSelectedForAction;
	}

	public void setViewSelected(boolean selected) {
		this.isSelectedForAction = selected;
		setFrameIcon(selected ? IconFactory.getSelectedIcon() : viewType.getIcon());
	}

	public JTable getTable() {
		return table;
	}

	public LogTableModel getModel() {
		return model;
	}

	public void updateFontSize(int newSize) {
		this.currentFontSize = newSize;
		Font font = table.getFont().deriveFont((float) newSize);
		table.setFont(font);
		table.setRowHeight(newSize + 4); // Add some padding
		columnManager.setupTableColumns(font); // Recalculate optimal widths for new font
		if (detailView != null) {
			detailView.setFontSize(newSize);
		}
	}

	private void attachFocusTrigger(JComponent component) {
		component.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				listener.onFocusGained(LogView.this);
			}
		});
	}

	public boolean hasTimestamps() {
		return entries.stream().anyMatch(e -> e.timestamp() != null);
	}

	public void setMetaData(String appName, String clientIp) {
		this.appName = appName;
		this.clientIp = clientIp;

		// Hide IP column for remote views where IP is in title
		if (viewType == ViewType.TCP
				|| (clientIp != null && !clientIp.isEmpty() && !"localhost".equals(clientIp)
						&& !"127.0.0.1".equals(clientIp))) {
			hideColumnPermanently(4);
		}
	}

	public String getAppName() {
		return appName;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setInitialLoggerName(String initialLoggerName) {
		this.initialLoggerName = initialLoggerName;
	}

	public String getInitialLoggerName() {
		return initialLoggerName;
	}

	public void addEntry(LogEntry entry) {
		SwingUtilities.invokeLater(() -> {
			boolean atBottom = isAtBottom();
			entries.add(entry);
			model.fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
			if (atBottom) {
				scrollToBottom();
			}
			if (filterPanel != null) {
				filterPanel.updateFilters();
			}
		});
	}

	private boolean isAtBottom() {
		JScrollBar sb = filteredTablePanel.getTableScrollPane().getVerticalScrollBar();
		return sb.getValue() + sb.getVisibleAmount() >= sb.getMaximum() - 20;
	}

	private void scrollToBottom() {
		JScrollBar sb = filteredTablePanel.getTableScrollPane().getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
	}

	public void hideColumnPermanently(int modelIndex) {
		columnManager.hideColumnPermanently(modelIndex);
	}

}
