package de.in.lsp.ui;

import de.in.lsp.model.LogEntry;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A self-contained UI component that displays log entries in a table with
 * filtering capabilities.
 */
public class LogView extends JPanel {

    public interface LogViewListener {
        void onClose(LogView view);

        void onMinimize(LogView view);

        void onMaximize(LogView view);

        void onFocusGained(LogView view);
    }

    private final LogTableModel model;
    private final JTable table;
    private final TableColumn[] allColumns;
    private final TableRowSorter<LogTableModel> sorter;
    private final List<LogEntry> entries;
    private final BiConsumer<LogView, LocalDateTime> onSelectionChanged;
    private final LogViewListener listener;
    private final JLabel titleLabel;
    private final JCheckBox selectionBox = new JCheckBox();
    private JPanel titleBar;
    private LogDetailView detailView;
    private JSplitPane splitPane;
    private JScrollPane tableScrollPane;
    private boolean maximized = false;
    private boolean focused = false;

    public LogView(List<LogEntry> entries, String title, BiConsumer<LogView, LocalDateTime> onSelectionChanged,
            LogViewListener listener) {
        this.entries = entries;
        this.titleLabel = new JLabel(title, SwingConstants.CENTER);
        this.titleLabel.setFont(this.titleLabel.getFont().deriveFont(Font.BOLD));
        this.onSelectionChanged = onSelectionChanged;
        this.listener = listener;
        this.model = new LogTableModel(entries);
        this.table = new JTable(model);
        this.sorter = new TableRowSorter<>(model);

        analyzeColumns();

        // Store all columns initially
        TableColumnModel tcm = table.getColumnModel();
        this.allColumns = new TableColumn[tcm.getColumnCount()];
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            allColumns[i] = tcm.getColumn(i);
        }

        setupUI();

        setupMouseListener();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Title Bar
        titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIManager.getColor("Button.background"));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setOpaque(false);
        selectionBox.setOpaque(false);
        leftPanel.add(selectionBox);
        attachFocusTrigger(selectionBox);

        titleBar.add(leftPanel, BorderLayout.WEST);
        titleBar.add(titleLabel, BorderLayout.CENTER);

        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                listener.onFocusGained(LogView.this);
            }
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        controls.setOpaque(false);

        JButton minBtn = createControlButton("-", "Minimize", e -> listener.onMinimize(this));
        JButton maxBtn = createControlButton("□", "Maximize", e -> {
            maximized = !maximized;
            listener.onMaximize(this);
        });
        JButton closeBtn = createControlButton("×", "Close", e -> listener.onClose(this));

        controls.add(minBtn);
        controls.add(maxBtn);
        controls.add(closeBtn);
        titleBar.add(controls, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        // Content Panel (Filter + Table)
        JPanel contentPanel = new JPanel(new BorderLayout());

        table.setDefaultRenderer(Object.class, new ZebraTableRenderer());
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(sorter);

        setupTableColumns();

        // Filter Field
        JTextField filterField = new JTextField();
        filterField.addActionListener(e -> {
            String text = filterField.getText();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter(text));
            }
        });
        attachFocusTrigger(filterField);

        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.add(new JLabel(" Filter (Regex): "), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        filterPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                listener.onFocusGained(LogView.this);
            }
        });

        createDetailView();

        tableScrollPane = new JScrollPane(table);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, null);
        splitPane.setResizeWeight(1.0);
        splitPane.setOneTouchExpandable(false);
        splitPane.setBorder(null);

        contentPanel.add(filterPanel, BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        // Increase Font Size (Ctrl + Plus and Ctrl + Equals)
        KeyStroke ctrlPlus = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PLUS,
                java.awt.event.InputEvent.CTRL_DOWN_MASK);
        KeyStroke ctrlEquals = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS,
                java.awt.event.InputEvent.CTRL_DOWN_MASK); // For keyboards where + is shift+=
        KeyStroke ctrlAdd = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD,
                java.awt.event.InputEvent.CTRL_DOWN_MASK); // Numpad +

        inputMap.put(ctrlPlus, "increaseFont");
        inputMap.put(ctrlEquals, "increaseFont");
        inputMap.put(ctrlAdd, "increaseFont");

        actionMap.put("increaseFont", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                changeFontSize(1);
            }
        });

        // Decrease Font Size (Ctrl + Minus)
        KeyStroke ctrlMinus = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS,
                java.awt.event.InputEvent.CTRL_DOWN_MASK);
        KeyStroke ctrlSubtract = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT,
                java.awt.event.InputEvent.CTRL_DOWN_MASK); // Numpad -

        inputMap.put(ctrlMinus, "decreaseFont");
        inputMap.put(ctrlSubtract, "decreaseFont");

        actionMap.put("decreaseFont", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                changeFontSize(-1);
            }
        });
    }

    private void changeFontSize(int delta) {
        Font font = table.getFont();
        int newSize = font.getSize() + delta;
        if (newSize >= 8 && newSize <= 40) {
            updateFontSize(newSize);
        }
    }

    private void createDetailView() {
        detailView = new LogDetailView(() -> toggleDetailView(false));
    }

    private void toggleDetailView(boolean show) {
        if (show) {
            if (splitPane.getBottomComponent() == null) {
                splitPane.setBottomComponent(detailView);
                splitPane.setDividerLocation(0.7);
            }
        } else {
            if (splitPane.getBottomComponent() != null) {
                splitPane.setBottomComponent(null);
            }
        }
    }

    private final Set<Integer> permanentlyHiddenColumns = new HashSet<>();

    private void analyzeColumns() {
        // 0: Timestamp
        if (!hasTimestamps())
            permanentlyHiddenColumns.add(0);

        // 1: Level
        boolean hasLevel = entries.stream().anyMatch(e -> e.level() != null && !e.level().isEmpty()
                && !"UNKNOWN".equalsIgnoreCase(e.level()));
        if (!hasLevel)
            permanentlyHiddenColumns.add(1);

        // 2: Thread
        boolean hasThread = entries.stream().anyMatch(e -> e.thread() != null && !e.thread().isEmpty()
                && !"UNKNOWN".equalsIgnoreCase(e.thread()));
        if (!hasThread)
            permanentlyHiddenColumns.add(2);

        // 3: Logger
        boolean hasLogger = entries.stream().anyMatch(e -> e.loggerName() != null && !e.loggerName().isEmpty()
                && !"UNKNOWN".equalsIgnoreCase(e.loggerName()));
        if (!hasLogger)
            permanentlyHiddenColumns.add(3);
    }

    private void setupTableColumns() {
        TableColumnModel tcm = table.getColumnModel();
        FontMetrics fm = table.getFontMetrics(table.getFont());

        // Helper to remove if hidden, else set width
        for (int i : new int[] { 0, 1, 2, 3 }) {
            TableColumn col = getColumnByModelIndex(tcm, i);
            if (col != null) {
                if (permanentlyHiddenColumns.contains(i)) {
                    tcm.removeColumn(col);
                    continue;
                }
                int width = calculateOptimalWidth(fm, i, 50);
                setColumnWidth(col, width);
            }
        }

        // 4: Message - Resizable (No max width)
        TableColumn colMsg = getColumnByModelIndex(tcm, 4);
        if (colMsg != null) {
            colMsg.setPreferredWidth(400);
            colMsg.setMinWidth(100);
            colMsg.setMaxWidth(Integer.MAX_VALUE);
        }

        // 5: Source - Fixed narrow, hide header
        TableColumn colSource = getColumnByModelIndex(tcm, 5);
        if (colSource != null) {
            setColumnWidth(colSource, 24);
            colSource.setHeaderValue(""); // No header text
        }
    }

    private TableColumn getColumnByModelIndex(TableColumnModel tcm, int modelIndex) {
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
            // Add column back (at roughly the right position if possible, but append is
            // easier)
            tcm.addColumn(allColumns[modelIndex]);
            // Re-sort view indices to match model indices for consistency
            sortViewColumnsByModelIndex();
            setupTableColumns(); // Ensure width/resizing logic is applied to the added column
        } else if (!visible && viewIndex != -1) {
            // Remove column
            tcm.removeColumn(tcm.getColumn(viewIndex));
        }
    }

    private void sortViewColumnsByModelIndex() {
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

    private JButton createControlButton(String text, String tooltip, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.setFocusable(false);
        btn.addActionListener(action);
        return btn;
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
                        } else if (modelColumn == 4) { // Message column
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
        int index = Collections.binarySearch(entries, new LogEntry(timestamp, null, null, null, null, null, null));
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
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        });
    }

    public boolean isMaximized() {
        return maximized;
    }

    public String getTitle() {
        return titleLabel.getText();
    }

    public List<LogEntry> getEntries() {
        return model.getEntries();
    }

    public boolean isSelected() {
        return selectionBox.isSelected();
    }

    public void setSelected(boolean selected) {
        selectionBox.setSelected(selected);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            if (titleBar != null)
                titleBar.setBackground(UIManager.getColor("Table.selectionBackground"));
            if (titleLabel != null)
                titleLabel.setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            if (titleBar != null)
                titleBar.setBackground(UIManager.getColor("Button.background"));
            if (titleLabel != null)
                titleLabel.setForeground(UIManager.getColor("Button.foreground"));
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public JTable getTable() {
        return table;
    }

    public LogTableModel getModel() {
        return model;
    }

    public void updateFontSize(int newSize) {
        Font font = table.getFont().deriveFont((float) newSize);
        table.setFont(font);
        table.setRowHeight(newSize + 4); // Add some padding
        setupTableColumns(); // Recalculate optimal widths for new font
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
}
