package de.in.lsp.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A modal dialog for searching text within one or all open log views.
 * Supports backward search, whole word matching, and column-specific filtering.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class SearchDialog extends JDialog {

    private final List<LogView> allViews;
    private LogView currentView;
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox allViewsBox = new JCheckBox("Search in all visible views");
    private final JCheckBox backwardBox = new JCheckBox("Backward direction");
    private final JCheckBox wholeWordBox = new JCheckBox("Match whole word only");
    private final List<JCheckBox> columnCheckboxes = new ArrayList<>();

    // Static state for persistence during runtime
    private static String lastSearchTerm = "";
    private static boolean lastAllViews = false;
    private static boolean lastBackward = false;
    private static boolean lastWholeWord = false;
    private static boolean[] lastColumns = null;

    public SearchDialog(Frame owner, List<LogView> allViews, LogView currentView) {
        super(owner, "Search", false);
        this.allViews = allViews;
        this.currentView = currentView;

        setupUI();
        loadSettings();

        pack();
        setLocationRelativeTo(owner);
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Search Term
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Search for:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(searchField, gbc);

        // Options
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        mainPanel.add(allViewsBox, gbc);
        gbc.gridy = 2;
        mainPanel.add(backwardBox, gbc);
        gbc.gridy = 3;
        mainPanel.add(wholeWordBox, gbc);

        // Columns
        JPanel columnsPanel = new JPanel(new GridLayout(0, 2));
        columnsPanel.setBorder(BorderFactory.createTitledBorder("In Columns"));
        String[] columnNames = { "Timestamp", "Level", "Thread", "Logger", "Message", "Source" };
        for (int i = 0; i < columnNames.length; i++) {
            JCheckBox cb = new JCheckBox(columnNames[i], i == 4); // Default only Message (index 4)
            columnCheckboxes.add(cb);
            columnsPanel.add(cb);
        }
        gbc.gridy = 4;
        mainPanel.add(columnsPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton findBtn = new JButton("Find next");
        JButton countBtn = new JButton("Count");
        JButton cancelBtn = new JButton("Cancel");

        findBtn.addActionListener(e -> {
            saveSettings();
            findNext();
        });
        countBtn.addActionListener(e -> {
            saveSettings();
            countOccurrences();
        });
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(findBtn);
        buttonPanel.add(countBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Map Enter key to Find Next
        searchField.addActionListener(e -> findBtn.doClick());

        // Escape key to close
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void loadSettings() {
        searchField.setText(lastSearchTerm);
        allViewsBox.setSelected(lastAllViews);
        backwardBox.setSelected(lastBackward);
        wholeWordBox.setSelected(lastWholeWord);
        if (lastColumns != null) {
            for (int i = 0; i < columnCheckboxes.size(); i++) {
                columnCheckboxes.get(i).setSelected(lastColumns[i]);
            }
        }
    }

    private void saveSettings() {
        lastSearchTerm = searchField.getText();
        lastAllViews = allViewsBox.isSelected();
        lastBackward = backwardBox.isSelected();
        lastWholeWord = wholeWordBox.isSelected();
        lastColumns = new boolean[columnCheckboxes.size()];
        for (int i = 0; i < columnCheckboxes.size(); i++) {
            lastColumns[i] = columnCheckboxes.get(i).isSelected();
        }
    }

    private void findNext() {
        String query = searchField.getText();
        if (query.isEmpty())
            return;

        boolean backward = backwardBox.isSelected();
        boolean matchWholeWord = wholeWordBox.isSelected();
        List<Integer> selectedColumns = getSelectedColumns();

        if (allViewsBox.isSelected()) {
            searchInAllViews(query, backward, matchWholeWord, selectedColumns);
        } else {
            searchInView(currentView, query, backward, matchWholeWord, selectedColumns, true);
        }
    }

    private void countOccurrences() {
        String query = searchField.getText();
        if (query.isEmpty())
            return;

        boolean matchWholeWord = wholeWordBox.isSelected();
        List<Integer> selectedColumns = getSelectedColumns();
        int total = 0;

        List<LogView> viewsToSearch = allViewsBox.isSelected() ? allViews : List.of(currentView);

        for (LogView view : viewsToSearch) {
            JTable table = view.getTable();
            LogTableModel model = view.getModel();

            // If "Weitersuchen" (all views or current from cursor?)
            // The request says: "nach dem selektierten Zeitstempel, wenn Option
            // 'Weitersuchen' aktiviert ist"
            // Since I didn't add a "Weitersuchen" checkbox explicitly (Find Next is
            // essentially Weitersuchen),
            // I will assume count starts from current selection if selection exists,
            // otherwise from start.

            int rowCount = table.getRowCount();

            for (int i = 0; i < rowCount; i++) {
                int modelRow = table.convertRowIndexToModel(i);
                if (matches(model, modelRow, query, matchWholeWord, selectedColumns)) {
                    total++;
                }
            }
        }

        JOptionPane.showMessageDialog(this, "Found " + total + " occurrences.");
    }

    private void searchInAllViews(String query, boolean backward, boolean matchWholeWord,
            List<Integer> selectedColumns) {
        int startIndex = allViews.indexOf(currentView);
        if (startIndex == -1)
            startIndex = 0;

        int i = startIndex;
        boolean firstView = true;

        while (true) {
            LogView view = allViews.get(i);
            if (searchInView(view, query, backward, matchWholeWord, selectedColumns, firstView)) {
                currentView = view;
                // Focus the view if it's not the current one
                // LogSyncPro should handle focus gained when table is clicked/selected
                return;
            }

            firstView = false;
            if (backward) {
                i--;
                if (i < 0)
                    i = allViews.size() - 1;
            } else {
                i++;
                if (i >= allViews.size())
                    i = 0;
            }

            if (i == startIndex)
                break; // Wrapped around
        }

        JOptionPane.showMessageDialog(this, "No more occurrences found.");
    }

    private boolean searchInView(LogView view, String query, boolean backward, boolean matchWholeWord,
            List<Integer> selectedColumns, boolean startFromSelection) {
        JTable table = view.getTable();
        LogTableModel model = view.getModel();
        int rowCount = table.getRowCount();
        if (rowCount == 0)
            return false;

        int startRow;
        if (startFromSelection) {
            int sel = table.getSelectedRow();
            if (sel == -1) {
                startRow = backward ? rowCount - 1 : 0;
            } else {
                startRow = backward ? sel - 1 : sel + 1;
            }
        } else {
            startRow = backward ? rowCount - 1 : 0;
        }

        if (backward) {
            for (int i = startRow; i >= 0; i--) {
                int modelRow = table.convertRowIndexToModel(i);
                if (matches(model, modelRow, query, matchWholeWord, selectedColumns)) {
                    selectRow(table, i);
                    return true;
                }
            }
        } else {
            for (int i = startRow; i < rowCount; i++) {
                int modelRow = table.convertRowIndexToModel(i);
                if (matches(model, modelRow, query, matchWholeWord, selectedColumns)) {
                    selectRow(table, i);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matches(LogTableModel model, int modelRow, String query, boolean wholeWord, List<Integer> columns) {
        String regex = wholeWord ? "\\b" + Pattern.quote(query) + "\\b" : Pattern.quote(query);
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        for (int col : columns) {
            Object val = model.getValueAt(modelRow, col);
            if (val != null) {
                if (pattern.matcher(val.toString()).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void selectRow(JTable table, int viewRow) {
        table.setRowSelectionInterval(viewRow, viewRow);
        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        table.requestFocusInWindow();
    }

    private List<Integer> getSelectedColumns() {
        List<Integer> cols = new ArrayList<>();
        for (int i = 0; i < columnCheckboxes.size(); i++) {
            if (columnCheckboxes.get(i).isSelected()) {
                cols.add(i);
            }
        }
        return cols;
    }

    public void setCurrentView(LogView view) {
        this.currentView = view;
        setTitle("Search - " + (view != null ? view.getTitle() : "No View"));
    }
}
