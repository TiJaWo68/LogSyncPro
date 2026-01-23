package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;

import de.in.lsp.service.SshK8sService;
import de.in.lsp.util.LspLogger;

/**
 * Dialog for selecting Kubernetes pods and containers to stream logs from.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class K8sPodSelectionDialog extends JDialog {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final List<SelectedContainer> selectedContainers = new ArrayList<>();
    private boolean confirmed = false;

    public K8sPodSelectionDialog(Frame parent, List<SshK8sService.K8sNamespace> data) {
        super(parent, "Select K8s Containers", true);
        setLayout(new BorderLayout());

        String[] columnNames = { "Select", "Namespace", "Pod", "Container" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        for (SshK8sService.K8sNamespace ns : data) {
            for (SshK8sService.K8sPod pod : ns.getPods()) {
                for (String container : pod.getContainers()) {
                    tableModel.addRow(new Object[] { false, ns.getName(), pod.getName(), container });
                }
            }
        }

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        K8sPodSelectionFilterPanel filterPanel = new K8sPodSelectionFilterPanel(table, sorter);
        FilteredTablePanel filteredTablePanel = new FilteredTablePanel(table, filterPanel);

        add(filteredTablePanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Import Selected");
        okButton.addActionListener(e -> {
            collectSelected();
            LspLogger.info(
                    "User confirmed selection of " + selectedContainers.size() + " containers from selection dialog.");
            confirmed = true;
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(600, 400);
        setLocationRelativeTo(parent);

        // Escape key to close
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void collectSelected() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                selectedContainers.add(new SelectedContainer(
                        (String) tableModel.getValueAt(i, 1),
                        (String) tableModel.getValueAt(i, 2),
                        (String) tableModel.getValueAt(i, 3)));
            }
        }
    }

    public List<SelectedContainer> getSelectedContainers() {
        return confirmed ? selectedContainers : new ArrayList<>();
    }

    public static class SelectedContainer {
        public final String namespace;
        public final String pod;
        public final String container;

        public SelectedContainer(String namespace, String pod, String container) {
            this.namespace = namespace;
            this.pod = pod;
            this.container = container;
        }
    }
}
