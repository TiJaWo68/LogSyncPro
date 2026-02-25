package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import de.in.lsp.service.SshK8sService.K8sNamespace;
import de.in.lsp.service.SshK8sService.K8sPod;

/**
 * Tests for the search text field filtering in K8sPodSelectionDialog. Verifies
 * that the global search field filters
 * across all columns, is case-insensitive, and that checkbox selections survive
 * filtering.
 * 
 * @author TiJaWo68 in cooperation with Claude Opus 4.6 using Antigravity
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
public class K8sPodSelectionFilterPanelTest {

    private K8sPodSelectionDialog createDialog() {
        List<K8sNamespace> data = new ArrayList<>();

        K8sNamespace nsApps = new K8sNamespace("apps");
        nsApps.addPod(new K8sPod("act4telerad-0", Arrays.asList("postgres", "istio-proxy")));
        nsApps.addPod(new K8sPod("duinsight-74c9d97fb4-prj9x", Arrays.asList("diagnost-client", "istio-proxy")));
        nsApps.addPod(
                new K8sPod("dicomservices-7db7d4b67b-sgtbm", Arrays.asList("dicomservices-server", "istio-proxy")));
        data.add(nsApps);

        K8sNamespace nsSystem = new K8sNamespace("kube-system");
        nsSystem.addPod(new K8sPod("coredns-5dd5bcc9f7-xsc7n", Arrays.asList("coredns")));
        data.add(nsSystem);

        return new K8sPodSelectionDialog(null, data);
    }

    @Test
    public void testSearchTextFiltersNamespace() {
        K8sPodSelectionDialog dialog = createDialog();
        JTable table = findTable(dialog);
        JTextField searchField = findSearchField(dialog);
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int totalRows = tableModel.getRowCount();
        assertTrue(totalRows > 0, "Table should have rows");

        // Search for "kube" -> should only show kube-system rows
        searchField.setText("kube");

        int visibleRows = table.getRowCount();
        assertTrue(visibleRows > 0, "Should have matching rows for 'kube'");
        assertTrue(visibleRows < totalRows, "Filtered view should have fewer rows");
        for (int i = 0; i < visibleRows; i++) {
            String ns = (String) table.getValueAt(i, 1);
            String pod = (String) table.getValueAt(i, 2);
            String container = (String) table.getValueAt(i, 3);
            boolean matches = ns.toLowerCase().contains("kube") || pod.toLowerCase().contains("kube")
                    || container.toLowerCase().contains("kube");
            assertTrue(matches, "Visible row should match search 'kube'");
        }

        // Clear search -> all rows visible
        searchField.setText("");
        assertEquals(totalRows, table.getRowCount(), "All rows should be visible after clearing search");

        dialog.dispose();
    }

    @Test
    public void testSearchTextFiltersPod() {
        K8sPodSelectionDialog dialog = createDialog();
        JTable table = findTable(dialog);
        JTextField searchField = findSearchField(dialog);

        searchField.setText("duinsight");

        int visibleRows = table.getRowCount();
        assertTrue(visibleRows > 0, "Should have matching rows for 'duinsight'");
        for (int i = 0; i < visibleRows; i++) {
            String pod = (String) table.getValueAt(i, 2);
            assertTrue(pod.toLowerCase().contains("duinsight"), "Visible pod should contain 'duinsight': " + pod);
        }

        dialog.dispose();
    }

    @Test
    public void testSearchIsCaseInsensitive() {
        K8sPodSelectionDialog dialog = createDialog();
        JTable table = findTable(dialog);
        JTextField searchField = findSearchField(dialog);

        searchField.setText("COREDNS");
        int visibleRows = table.getRowCount();
        assertTrue(visibleRows > 0, "Case-insensitive search should find 'coredns' rows with uppercase input");

        dialog.dispose();
    }

    @Test
    public void testSelectionSurvivesSearchFiltering() {
        K8sPodSelectionDialog dialog = createDialog();
        JTable table = findTable(dialog);
        JTextField searchField = findSearchField(dialog);
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int totalRows = tableModel.getRowCount();

        // Select "postgres" container rows in the model
        for (int i = 0; i < totalRows; i++) {
            String container = (String) tableModel.getValueAt(i, 3);
            if ("postgres".equals(container)) {
                tableModel.setValueAt(true, i, 0);
            }
        }

        // Filter to only show "coredns" rows -> postgres rows hidden
        searchField.setText("coredns");
        assertTrue(table.getRowCount() < totalRows, "Some rows should be hidden");

        // Clear search -> all rows visible again
        searchField.setText("");
        assertEquals(totalRows, table.getRowCount(), "All rows visible after clearing search");

        // Verify the selected postgres row is still selected
        boolean foundSelectedPostgres = false;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String container = (String) tableModel.getValueAt(i, 3);
            if ("postgres".equals(container)) {
                assertTrue((Boolean) tableModel.getValueAt(i, 0),
                        "Postgres row should still be selected after search round-trip");
                foundSelectedPostgres = true;
            }
        }
        assertTrue(foundSelectedPostgres, "Should find at least one selected postgres row");

        dialog.dispose();
    }

    @Test
    public void testSearchNoMatch() {
        K8sPodSelectionDialog dialog = createDialog();
        JTable table = findTable(dialog);
        JTextField searchField = findSearchField(dialog);
        int totalRows = ((DefaultTableModel) table.getModel()).getRowCount();

        searchField.setText("zzz_nonexistent_zzz");
        assertEquals(0, table.getRowCount(), "No rows should match a nonexistent search term");

        // Clear -> back to normal
        searchField.setText("");
        assertEquals(totalRows, table.getRowCount(), "Rows should reappear after clearing search");

        dialog.dispose();
    }

    private JTable findTable(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof JTable) {
                return (JTable) comp;
            }
            if (comp instanceof java.awt.Container) {
                JTable found = findTable((java.awt.Container) comp);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private JTextField findSearchField(java.awt.Container container) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof JTextField tf) {
                // The search field has a specific tooltip
                if ("Search across all columns (substring, case-insensitive)".equals(tf.getToolTipText())) {
                    return tf;
                }
            }
            if (comp instanceof java.awt.Container) {
                JTextField found = findSearchField((java.awt.Container) comp);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
