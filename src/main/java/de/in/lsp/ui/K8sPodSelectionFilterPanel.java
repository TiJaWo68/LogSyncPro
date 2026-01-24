package de.in.lsp.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Filter panel for K8sPodSelectionDialog.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class K8sPodSelectionFilterPanel extends AbstractTableFilterPanel<DefaultTableModel> {

	private MultiSelectFilter namespaceFilter;
	private MultiSelectFilter podFilter;
	private MultiSelectFilter containerFilter;
	private JCheckBox selectAllCheckBox;

	public K8sPodSelectionFilterPanel(JTable table, TableRowSorter<DefaultTableModel> sorter) {
		super(table, sorter);
		createFilterComponents();
		updateFilterOptions();
		updateSelectAllState();

		table.getModel().addTableModelListener(e -> {
			if (e.getColumn() == 0 || e.getColumn() == javax.swing.event.TableModelEvent.ALL_COLUMNS) {
				updateSelectAllState();
			}
		});
	}

	private void createFilterComponents() {
		namespaceFilter = new MultiSelectFilter("Namespace", opts -> applyFilters());
		podFilter = new MultiSelectFilter("Pod", opts -> applyFilters());
		containerFilter = new MultiSelectFilter("Container", opts -> applyFilters());

		selectAllCheckBox = new JCheckBox();
		selectAllCheckBox.setOpaque(false);
		selectAllCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		selectAllCheckBox.setToolTipText("Select / Deselect All Visible");
		selectAllCheckBox.addActionListener(e -> toggleSelectAll());
	}

	@Override
	protected void onTableDataChanged() {
		updateFilterOptions();
		updateSelectAllState();
	}

	private void updateSelectAllState() {
		if (selectAllCheckBox == null)
			return;
		boolean allSelected = table.getRowCount() > 0;
		for (int i = 0; i < table.getRowCount(); i++) {
			if (!(Boolean) table.getValueAt(i, 0)) {
				allSelected = false;
				break;
			}
		}
		// Show "Checked" icon if not all are selected (suggesting "Select All") Show "Unchecked" icon if all are selected (suggesting
		// "Deselect All")
		selectAllCheckBox.setSelected(!allSelected);
	}

	/**
	 * Extracts the human-readable base name of a pod by removing random suffixes. e.g. "auditrepository-7fcd847b65-rkdlp" ->
	 * "auditrepository" e.g. "act4telerad-0" -> "act4telerad"
	 */
	private String getPodBaseName(String podName) {
		if (podName == null)
			return "";
		// Strip deployment hash + random suffix: -[8-10 chars]-[5 chars] Strip statefulset/job index: -[digits]
		return podName.replaceAll("-(?:[a-z0-9]{8,15}-[a-z0-9]{5}|[0-9]+)$", "");
	}

	public void updateFilterOptions() {
		Set<String> namespaces = new HashSet<>();
		Set<String> pods = new HashSet<>();
		Set<String> containers = new HashSet<>();

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			String ns = (String) model.getValueAt(i, 1);
			String pod = (String) model.getValueAt(i, 2);
			String container = (String) model.getValueAt(i, 3);
			String podBase = getPodBaseName(pod);

			// Faceted search: only exclude the filter itself from the visible set check
			boolean matchesNamespace = !namespaceFilter.isActive() || namespaceFilter.getSelectedOptions().contains(ns);
			boolean matchesPod = !podFilter.isActive() || podFilter.getSelectedOptions().contains(podBase);
			boolean matchesContainer = !containerFilter.isActive() || containerFilter.getSelectedOptions().contains(container);

			if (matchesPod && matchesContainer) {
				namespaces.add(ns);
			}
			if (matchesNamespace && matchesContainer) {
				pods.add(podBase);
			}
			if (matchesNamespace && matchesPod) {
				containers.add(container);
			}
		}

		namespaceFilter.setOptions(namespaces);
		podFilter.setOptions(pods);
		containerFilter.setOptions(containers);
	}

	private void applyFilters() {
		if (isUpdating)
			return;
		isUpdating = true;
		try {
			List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();

			if (namespaceFilter.isActive()) {
				Set<String> selected = namespaceFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
						return selected.contains(entry.getStringValue(1));
					}
				});
			}

			if (podFilter.isActive()) {
				Set<String> selected = podFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
						return selected.contains(getPodBaseName(entry.getStringValue(2)));
					}
				});
			}

			if (containerFilter.isActive()) {
				Set<String> selected = containerFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
						return selected.contains(entry.getStringValue(3));
					}
				});
			}

			if (filters.isEmpty()) {
				sorter.setRowFilter(null);
			} else {
				sorter.setRowFilter(RowFilter.andFilter(filters));
			}
		} finally {
			isUpdating = false;
		}
	}

	private void toggleSelectAll() {
		boolean anyDeselected = false;
		for (int i = 0; i < table.getRowCount(); i++) {
			if (!(Boolean) table.getValueAt(i, 0)) {
				anyDeselected = true;
				break;
			}
		}
		// If there's an unchecked row, clicking should check everything. If all are checked, clicking should uncheck everything.
		boolean newState = anyDeselected || table.getRowCount() == 0;
		for (int i = 0; i < table.getRowCount(); i++) {
			table.setValueAt(newState, i, 0);
		}
	}

	@Override
	protected JComponent getComponentForColumn(int modelIndex) {
		return switch (modelIndex) {
		case 0 -> selectAllCheckBox;
		case 1 -> namespaceFilter;
		case 2 -> podFilter;
		case 3 -> containerFilter;
		default -> {
			JPanel p = new JPanel();
			p.setOpaque(false);
			yield p;
		}
		};
	}
}
