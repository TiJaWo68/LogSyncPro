package de.in.lsp.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import de.in.lsp.model.LogEntry;

/**
 * A panel that displays a row of filters aligned with table columns.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewFilterPanel extends JPanel {

    private final JTable table;
    private final TableRowSorter<LogTableModel> sorter;
    private final List<LogEntry> entries;

    private MultiSelectFilter levelFilter;
    private MultiSelectFilter threadFilter;
    private MultiSelectFilter loggerFilter;
    private JTextField messageFilterField;

    private boolean isUpdatingFilters = false;

    public LogViewFilterPanel(JTable table, TableRowSorter<LogTableModel> sorter,
            List<LogEntry> entries) {
        this.table = table;
        this.sorter = sorter;
        this.entries = entries;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        createFilterComponents();
    }

    private void createFilterComponents() {
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
    }

    public void updateFilters() {
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
        applyFilters();
    }

    public void applyFilters() {
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

    public void updateAlignment() {
        removeAll();

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

            comp.setBorder(BorderFactory.createCompoundBorder(
                    UIManager.getBorder("TableHeader.cellBorder"),
                    BorderFactory.createEmptyBorder(0, 5, 0, 2)));
            if (comp.getBorder() == null) {
                comp.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
                        BorderFactory.createEmptyBorder(0, 5, 0, 5)));
            }

            add(comp);
        }

        revalidate();
        repaint();
    }

    private void toggleMessageFilter(boolean showField) {
        messageFilterField.setVisible(showField);
        updateAlignment();
        if (showField) {
            SwingUtilities.invokeLater(() -> messageFilterField.requestFocusInWindow());
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

    private void styleAsHeaderButton(JButton btn) {
        btn.setFocusable(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        Font font = UIManager.getFont("TableHeader.font");
        if (font != null) {
            btn.setFont(font);
        } else {
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        }
        Color fg = UIManager.getColor("TableHeader.foreground");
        if (fg != null) {
            btn.setForeground(fg);
        } else {
            btn.setForeground(Color.LIGHT_GRAY);
        }
        btn.setHorizontalAlignment(JButton.LEFT);
    }

    public JTextField getMessageFilterField() {
        return messageFilterField;
    }
}
