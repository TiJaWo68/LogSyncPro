package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import de.in.lsp.service.SshK8sService.K8sNamespace;
import de.in.lsp.service.SshK8sService.K8sPod;

/**
 * Tests for K8sPodSelectionDialog.
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
public class K8sPodSelectionDialogTest {

	@Test
	public void testColumnWidths() {
		// Setup data
		List<K8sNamespace> data = new ArrayList<>();
		K8sNamespace ns = new K8sNamespace("default");
		ns.addPod(new K8sPod("pod1", Arrays.asList("container1")));
		data.add(ns);

		// Create dialog
		K8sPodSelectionDialog dialog = new K8sPodSelectionDialog(null, data);

		// Access table (via looking at components, since table is private)
		JTable table = findTable(dialog);
		assertTrue(table != null, "Table should be found in dialog");

		// Verify first column width fixed to 25
		TableColumn selectColumn = table.getColumnModel().getColumn(0);
		assertEquals(K8sPodSelectionDialog.CHECKBOX_COL_WIDTH, selectColumn.getMinWidth(),
				"Min width should be " + K8sPodSelectionDialog.CHECKBOX_COL_WIDTH);
		assertEquals(K8sPodSelectionDialog.CHECKBOX_COL_WIDTH, selectColumn.getMaxWidth(),
				"Max width should be " + K8sPodSelectionDialog.CHECKBOX_COL_WIDTH);
		assertEquals(K8sPodSelectionDialog.CHECKBOX_COL_WIDTH, selectColumn.getPreferredWidth(),
				"Preferred width should be " + K8sPodSelectionDialog.CHECKBOX_COL_WIDTH);

		// Mock size and perform layout to verify actual widths
		dialog.setSize(K8sPodSelectionDialog.DIALOG_WIDTH, K8sPodSelectionDialog.DIALOG_HEIGHT);
		dialog.doLayout();
		table.setSize(K8sPodSelectionDialog.DIALOG_WIDTH, K8sPodSelectionDialog.DIALOG_HEIGHT); // Usually happens by
																								// layout manager
		table.doLayout();

		// Verify resizing mode (should be default/all columns)
		assertEquals(JTable.AUTO_RESIZE_ALL_COLUMNS, table.getAutoResizeMode(),
				"Auto resize mode should allow other columns to share space");

		// Verify relative distribution (20-40-40 split for remaining columns) Namespace
		// (1) : Pod (2) : Container (3) should be 1 : 2 : 2
		// We verify actual widths now
		int nsWidth = table.getColumnModel().getColumn(1).getWidth();
		int podWidth = table.getColumnModel().getColumn(2).getWidth();
		int containerWidth = table.getColumnModel().getColumn(3).getWidth();

		// Expected widths based on calculation:
		int remainingWidth = K8sPodSelectionDialog.DIALOG_WIDTH - K8sPodSelectionDialog.CHECKBOX_COL_WIDTH;
		int expectedNsWidth = (int) (remainingWidth * 0.2);
		int expectedPodWidth = (int) (remainingWidth * 0.4);

		// Allow tolerances for layout managers and LookAndFeel insets
		assertTrue(Math.abs(nsWidth - expectedNsWidth) <= 5,
				"Namespace width mismatch: expected " + expectedNsWidth + ", got " + nsWidth);
		assertTrue(Math.abs(podWidth - expectedPodWidth) <= 5,
				"Pod width mismatch: expected " + expectedPodWidth + ", got " + podWidth);

		// Container width handles remainder, so check it's close to pod width
		assertTrue(Math.abs(containerWidth - podWidth) <= 5,
				"Container width (" + containerWidth + ") should be similar to Pod width (" + podWidth + ")");

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
}
