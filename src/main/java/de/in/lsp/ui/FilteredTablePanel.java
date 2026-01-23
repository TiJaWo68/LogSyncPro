package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

/**
 * A reusable panel that combines a JTable with an AbstractTableFilterPanel.
 * It handles horizontal scroll synchronization and alignment updates.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class FilteredTablePanel extends JPanel {

    private final JTable table;
    private final AbstractTableFilterPanel<?> filterPanel;
    private final JScrollPane tableScrollPane;
    private final JScrollPane headerScroll;

    public FilteredTablePanel(JTable table, AbstractTableFilterPanel<?> filterPanel) {
        setLayout(new BorderLayout());
        this.table = table;
        this.filterPanel = filterPanel;

        table.setTableHeader(null);
        tableScrollPane = new JScrollPane(table);

        headerScroll = new JScrollPane(filterPanel);
        headerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        headerScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        headerScroll.setBorder(null);
        headerScroll.setOpaque(false);
        headerScroll.getViewport().setOpaque(false);

        // Synchronize horizontal scroll
        tableScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            headerScroll.getHorizontalScrollBar().setValue(e.getValue());
        });

        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.setOpaque(false);
        headerContainer.add(headerScroll, BorderLayout.CENTER);

        add(headerContainer, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);

        // Ensure alignment is updated on structural changes
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                filterPanel.updateAlignment();
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
                filterPanel.updateAlignment();
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
                filterPanel.updateAlignment();
            }

            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                filterPanel.updateAlignment();
            }

            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });

        tableScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                filterPanel.updateAlignment();
            }
        });
    }

    public JTable getTable() {
        return table;
    }

    public AbstractTableFilterPanel<?> getFilterPanel() {
        return filterPanel;
    }

    public JScrollPane getTableScrollPane() {
        return tableScrollPane;
    }
}
