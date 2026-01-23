package de.in.lsp.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Base class for a panel that displays filter components aligned with JTable
 * columns.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public abstract class AbstractTableFilterPanel<M extends TableModel> extends JPanel {

    protected final JTable table;
    protected final TableRowSorter<M> sorter;

    protected boolean isUpdating = false;

    public AbstractTableFilterPanel(JTable table, TableRowSorter<M> sorter) {
        this.table = table;
        this.sorter = sorter;

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                updateAlignment();
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
                updateAlignment();
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
                updateAlignment();
            }

            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                updateAlignment();
            }

            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });

        sorter.addRowSorterListener(e -> {
            if (e.getType() == javax.swing.event.RowSorterEvent.Type.SORTED) {
                javax.swing.SwingUtilities.invokeLater(this::onTableDataChanged);
            }
        });
    }

    /**
     * Called when table data changes or filters are applied.
     * Subclasses should override this to refresh their filter options.
     * Implementation should ideally use faceted search logic (cross-filtering).
     */
    protected void onTableDataChanged() {
        // Default: do nothing
    }

    /**
     * Updates the alignment of filter components with table columns.
     */
    public void updateAlignment() {
        removeAll();

        TableColumnModel tcm = table.getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            TableColumn col = tcm.getColumn(i);
            int modelIndex = col.getModelIndex();
            int width = col.getWidth();

            JComponent comp = getComponentForColumn(modelIndex);

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

    /**
     * Subclasses must provide the filter component for the given model column
     * index.
     */
    protected abstract JComponent getComponentForColumn(int modelIndex);

    protected JComponent createHeaderLabel(String text) {
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

    protected void styleAsHeaderButton(JButton btn) {
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
}
