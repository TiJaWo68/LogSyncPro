package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import de.in.lsp.model.LogEntry;
/**
 * A self-contained UI component that displays log entries in a table with
 * filtering capabilities.
 * 
/**
 * A self-contained UI component that displays log entries in a table with
 * filtering capabilities.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
import de.in.lsp.ui.helper.LogViewColumnManager;

public class LogView extends JInternalFrame {

    public enum ViewType {
        INTERNAL("icons/internal.svg"),
        MERGED("icons/logo.svg"),
        K8S("icons/network.svg"),
        TCP("icons/tcp.svg"),
        FILE("icons/file.svg");

        private final String iconPath;

        ViewType(String iconPath) {
            this.iconPath = iconPath;
        }

        public String getIconPath() {
            return iconPath;
        }
    }

    public interface LogViewListener {
        void onClose(LogView view);

        void onMinimize(LogView view);

        void onMaximize(LogView view);

        void onFocusGained(LogView view);
    }

    private final LogTableModel model;
    private final JTable table;
    private final LogViewColumnManager columnManager;
    private final TableRowSorter<LogTableModel> sorter;
    private final List<LogEntry> entries;
    private final BiConsumer<LogView, LocalDateTime> onSelectionChanged;
    private final LogViewListener listener;
    private LogDetailView detailView;
    private JSplitPane splitPane;
    private JScrollPane tableScrollPane;
    private boolean maximized = false;
    private String appName;
    private String clientIp;
    private String initialLoggerName;
    private ViewType viewType;

    private JPanel filterRow;
    private MultiSelectFilter levelFilter;
    private MultiSelectFilter threadFilter;
    private MultiSelectFilter loggerFilter;
    private JTextField messageFilterField;

    private boolean isSelectedForAction = false; // Internal selection state

    public LogView(List<LogEntry> entries, String title, BiConsumer<LogView, LocalDateTime> onSelectionChanged,
            LogViewListener listener, ViewType viewType) {
        super(title, true, true, true, true);
        // putClientProperty("JInternalFrame.titleAlignment", "center"); // Removed
        // centering
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
        setFrameIcon(getNormalIcon());

        // Use InternalFrameListener instead of custom listeners where possible
        addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent e) {
                listener.onFocusGained(LogView.this);
            }

            @Override
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
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
        setBorder(null); // Let DesktopPane handle borders

        createDetailView();

        // Content Panel (Filter + Table)
        JPanel contentPanel = new JPanel(new BorderLayout());

        columnManager.setupTableColumns();

        // Explicitly set renderer for all columns
        ZebraTableRenderer renderer = new ZebraTableRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        // Definitively remove standard header from the table before adding to scroll
        // pane
        table.setTableHeader(null);
        tableScrollPane = new JScrollPane(table);

        // Create a professional header panel that mimics JTableHeader
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Paint a subtle gradient or solid color consistent with TableHeader
                g.setColor(UIManager.getColor("TableHeader.background"));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.GRAY);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setPreferredSize(new Dimension(100, 26));

        // Wrap header in a scroll pane that syncs with the table
        JScrollPane headerScroll = new JScrollPane(headerPanel);
        headerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        headerScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        headerScroll.setBorder(null);
        headerScroll.setOpaque(false);
        headerScroll.getViewport().setOpaque(false);

        // Sync horizontal scroll
        tableScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            headerScroll.getHorizontalScrollBar().setValue(e.getValue());
        });

        contentPanel.add(headerScroll, BorderLayout.NORTH);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, null);
        splitPane.setResizeWeight(1.0);
        splitPane.setOneTouchExpandable(false);
        splitPane.setBorder(null);

        contentPanel.add(splitPane, BorderLayout.CENTER);

        // Update alignment regularly
        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {
                updateFilterRowAlignment();
            }

            @Override
            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {
                updateFilterRowAlignment();
            }

            @Override
            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {
                updateFilterRowAlignment();
            }

            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                updateFilterRowAlignment();
            }

            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });

        tableScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustMessageColumnWidth();
                updateFilterRowAlignment();
            }
        });

        this.filterRow = headerPanel; // Link filterRow for updates
        createFilterRow(); // Now populate the new filterRow
        updateFilterRowAlignment();

        add(contentPanel, BorderLayout.CENTER);

        setupKeyBindings();
    }

    private void adjustMessageColumnWidth() {
        TableColumnModel tcm = table.getColumnModel();
        int totalWidth = tableScrollPane.getViewport().getWidth();
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
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        });
    }

    public boolean isMaximized() {
        return maximized;
    }

    public String getTitle() {
        return super.getTitle();
    }

    public List<LogEntry> getEntries() {
        return model.getEntries();
    }

    private void setupTitleBarListener() {
        // Try to add mouse listener to the title pane (NorthPane)
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

    private Icon getNormalIcon() {
        try {
            String path = (viewType != null ? viewType.getIconPath() : ViewType.FILE.getIconPath());
            return new com.formdev.flatlaf.extras.FlatSVGIcon(path, 16, 16);
        } catch (Exception e) {
            return null;
        }
    }

    private Icon getSelectedIcon() {
        try {
            return new com.formdev.flatlaf.extras.FlatSVGIcon("icons/selected.svg", 16, 16);
        } catch (Exception e) {
            return getNormalIcon(); // Fallback
        }
    }

    public boolean isViewSelected() {
        return isSelectedForAction;
    }

    public void setViewSelected(boolean selected) {
        this.isSelectedForAction = selected;
        if (selected) {
            setFrameIcon(getSelectedIcon());
        } else {
            setFrameIcon(getNormalIcon());
        }
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
        columnManager.setupTableColumns(); // Recalculate optimal widths for new font
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
        if (viewType == ViewType.TCP || (clientIp != null && !clientIp.isEmpty() && !"localhost".equals(clientIp)
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
        entries.add(entry);
        model.addEntry(entry);

        boolean needsFilterUpdate = false;
        if (levelFilter != null) {
            if (entry.level() != null && !entry.level().isEmpty()
                    && !levelFilter.getAllOptions().contains(entry.level())) {
                needsFilterUpdate = true;
            } else if (entry.getSimpleThreadName() != null && !entry.getSimpleThreadName().isEmpty()
                    && !threadFilter.getAllOptions().contains(entry.getSimpleThreadName())) {
                needsFilterUpdate = true;
            } else if (entry.getSimpleLoggerName() != null && !entry.getSimpleLoggerName().isEmpty()
                    && !loggerFilter.getAllOptions().contains(entry.getSimpleLoggerName())) {
                needsFilterUpdate = true;
            }
        }

        // If sorting is active, the sorter will handle it.
        // But we might want to auto-scroll if we were at the bottom.
        boolean atBottom = isAtBottom();
        if (atBottom) {
            SwingUtilities.invokeLater(this::scrollToBottom);
        }

        if (needsFilterUpdate) {
            updateFilters();
        }
    }

    public void hideColumnPermanently(int modelIndex) {
        columnManager.hideColumnPermanently(modelIndex);
    }

    private void createFilterRow() {
        levelFilter = new MultiSelectFilter("Level", opts -> applyFilters());
        threadFilter = new MultiSelectFilter("Thread", opts -> applyFilters());
        loggerFilter = new MultiSelectFilter("Logger", opts -> applyFilters());

        messageFilterField = new JTextField();
        messageFilterField.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        messageFilterField.setVisible(false);
        messageFilterField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (messageFilterField.getText().isEmpty())
                    toggleMessageFilter(false);
            }
        });
        messageFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }
        });
        attachFocusTrigger(messageFilterField);

        updateFilterRowAlignment();
        updateFilters();
    }

    private void toggleMessageFilter(boolean showField) {
        messageFilterField.setVisible(showField);
        updateFilterRowAlignment();
        if (showField) {
            SwingUtilities.invokeLater(() -> messageFilterField.requestFocusInWindow());
        }
    }

    private void updateFilterRowAlignment() {
        if (filterRow == null)
            return;

        // Ensure filters are initialized
        if (levelFilter == null) {
            levelFilter = new MultiSelectFilter("Level", opts -> applyFilters());
            threadFilter = new MultiSelectFilter("Thread", opts -> applyFilters());
            loggerFilter = new MultiSelectFilter("Logger", opts -> applyFilters());
            messageFilterField = new JTextField();
            messageFilterField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (messageFilterField.getText().isEmpty())
                        toggleMessageFilter(false);
                }
            });
            messageFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    applyFilters();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    applyFilters();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    applyFilters();
                }
            });
        }

        filterRow.removeAll();

        TableColumnModel tcm = table.getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            TableColumn col = tcm.getColumn(i);
            int modelIndex = col.getModelIndex();
            int width = col.getWidth();

            JComponent comp;
            switch (modelIndex) {
                case 0 -> comp = createHeaderLabel("Timestamp");
                case 1 -> comp = levelFilter;
                case 2 -> comp = threadFilter;
                case 3 -> comp = loggerFilter;
                case 4 -> comp = createHeaderLabel("IP");
                case 5 -> {
                    if (messageFilterField.isVisible()) {
                        comp = messageFilterField;
                    } else {
                        JButton btn = new JButton("Message");
                        styleAsHeaderButton(btn);
                        if (!messageFilterField.getText().isEmpty()) {
                            btn.setIcon(new FilterIcon(Color.GRAY));
                        }
                        btn.addActionListener(e -> toggleMessageFilter(true));
                        comp = btn;
                    }
                }
                case 6 -> comp = createHeaderLabel("");
                default -> {
                    comp = new JPanel();
                    comp.setOpaque(false);
                }
            }

            comp.setPreferredSize(new Dimension(width, 26));
            comp.setMinimumSize(new Dimension(width, 26));
            comp.setMaximumSize(new Dimension(width, 26));

            // Add a proper header cell border
            comp.setBorder(BorderFactory.createCompoundBorder(
                    UIManager.getBorder("TableHeader.cellBorder"),
                    BorderFactory.createEmptyBorder(0, 5, 0, 2)));
            if (comp.getBorder() == null) {
                comp.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
                        BorderFactory.createEmptyBorder(0, 5, 0, 5)));
            }

            filterRow.add(comp);
        }

        filterRow.revalidate();
        filterRow.repaint();
    }

    private void styleAsHeaderButton(JButton btn) {
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        Font headerFont = UIManager.getFont("TableHeader.font");
        if (headerFont != null)
            btn.setFont(headerFont);
        else
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));

        Color headerFg = UIManager.getColor("TableHeader.foreground");
        if (headerFg != null)
            btn.setForeground(headerFg);
        else
            btn.setForeground(Color.LIGHT_GRAY);
    }

    private static class FilterIcon implements Icon {
        private final Color color;

        FilterIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int[] px = { x + 2, x + 10, x + 10, x + 6, x + 6, x + 2 };
            int[] py = { y + 2, y + 2, y + 6, y + 10, y + 6, y + 6 };
            g2.fillPolygon(px, py, 6);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    private JComponent createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        Font headerFont = UIManager.getFont("TableHeader.font");
        if (headerFont != null)
            label.setFont(headerFont);
        else
            label.setFont(label.getFont().deriveFont(Font.BOLD));

        Color headerFg = UIManager.getColor("TableHeader.foreground");
        if (headerFg != null)
            label.setForeground(headerFg);
        else
            label.setForeground(Color.LIGHT_GRAY);

        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        return label;
    }

    private boolean isUpdatingFilters = false;

    private void updateFilters() {
        if (levelFilter == null)
            return;

        isUpdatingFilters = true;
        try {
            Set<String> levels = entries.stream()
                    .map(LogEntry::level)
                    .filter(l -> l != null && !l.isEmpty())
                    .collect(Collectors.toSet());
            levelFilter.setOptions(levels);

            Set<String> threads = entries.stream()
                    .map(LogEntry::getSimpleThreadName)
                    .filter(t -> t != null && !t.isEmpty())
                    .collect(Collectors.toSet());
            threadFilter.setOptions(threads);

            Set<String> loggers = entries.stream()
                    .map(LogEntry::getSimpleLoggerName)
                    .filter(l -> l != null && !l.isEmpty())
                    .collect(Collectors.toSet());
            loggerFilter.setOptions(loggers);
        } finally {
            isUpdatingFilters = false;
        }
        // Apply once at the end if anything actually changed
        applyFilters();
    }

    private void applyFilters() {
        if (isUpdatingFilters)
            return;

        List<RowFilter<LogTableModel, Integer>> filters = new ArrayList<>();

        if (levelFilter.isActive()) {
            Set<String> selected = levelFilter.getSelectedOptions();
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(1);
                    return val != null && selected.contains(val.toString());
                }
            });
        }

        if (threadFilter.isActive()) {
            Set<String> selected = threadFilter.getSelectedOptions();
            filters.add(new RowFilter<>() {

                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(2);
                    return val != null && selected.contains(val.toString());
                }
            });
        }

        if (loggerFilter.isActive()) {
            Set<String> selected = loggerFilter.getSelectedOptions();
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(3);
                    return val != null && selected.contains(val.toString());
                }
            });
        }

        String msgText = messageFilterField.getText();
        if (!msgText.isEmpty()) {
            try {
                filters.add(RowFilter.regexFilter(msgText, 5));
            } catch (java.util.regex.PatternSyntaxException e) {
                // Ignore invalid regex
            }
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private boolean isAtBottom() {
        JScrollBar sb = tableScrollPane.getVerticalScrollBar();
        return sb.getValue() + sb.getVisibleAmount() >= sb.getMaximum() - 20;
    }

    private void scrollToBottom() {
        JScrollBar sb = tableScrollPane.getVerticalScrollBar();
        sb.setValue(sb.getMaximum());
    }

}
