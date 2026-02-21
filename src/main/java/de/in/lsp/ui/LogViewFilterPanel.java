package de.in.lsp.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

import de.in.lsp.model.LogEntry;

/**
 * A panel that displays a row of filters aligned with table columns.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewFilterPanel extends AbstractTableFilterPanel<LogTableModel> {

	private MultiSelectFilter levelFilter;
	private MultiSelectFilter threadFilter;
	private MultiSelectFilter loggerFilter;
	private MultiSelectFilter ipFilter;
	private MultiSelectFilter sourceFilter;
	private JTextField messageFilterField;

	public LogViewFilterPanel(JTable table, TableRowSorter<LogTableModel> sorter, List<LogEntry> entries) {
		super(table, sorter);
		createFilterComponents();
	}

	private void createFilterComponents() {
		levelFilter = new MultiSelectFilter("Level", opts -> applyFilters());
		threadFilter = new MultiSelectFilter("Thread", opts -> applyFilters());
		loggerFilter = new MultiSelectFilter("Logger", opts -> applyFilters());
		ipFilter = new MultiSelectFilter("IP", opts -> applyFilters());
		sourceFilter = new MultiSelectFilter("Source", opts -> applyFilters());

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

	@Override
	protected void onTableDataChanged() {
		updateFilters();
	}

	public void updateFilters() {
		if (levelFilter == null)
			return;

		Set<String> levels = new HashSet<>();
		Set<String> threads = new HashSet<>();
		Set<String> loggers = new HashSet<>();
		Set<String> ips = new HashSet<>();
		Set<String> sources = new HashSet<>();

		LogTableModel model = (LogTableModel) table.getModel();
		List<LogEntry> entries = model.getEntries();

		String msgText = messageFilterField.getText();

		for (LogEntry entry : entries) {
			String level = entry.level();
			String thread = entry.getSimpleThreadName();
			String logger = entry.getSimpleLoggerName();
			String msg = entry.message();

			// Faceted search checks
			boolean matchesMessage = msgText.isEmpty();
			if (!matchesMessage) {
				try {
					matchesMessage = java.util.regex.Pattern.compile(msgText, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(msg).find();
				} catch (Exception e) {
					matchesMessage = true;
				}
			}

			boolean matchesLevel = !levelFilter.isActive() || levelFilter.getSelectedOptions().contains(level);
			boolean matchesThread = !threadFilter.isActive() || threadFilter.getSelectedOptions().contains(thread);
			boolean matchesLogger = !loggerFilter.isActive() || loggerFilter.getSelectedOptions().contains(logger);
			boolean matchesIp = !ipFilter.isActive() || ipFilter.getSelectedOptions().contains(entry.ip());
			boolean matchesSource = !sourceFilter.isActive() || sourceFilter.getSelectedOptions().contains(entry.sourceFile());

			// Add to options if other filters match
			if (matchesThread && matchesLogger && matchesIp && matchesSource && matchesMessage) {
				if (level != null && !level.isEmpty())
					levels.add(level);
			}
			if (matchesLevel && matchesLogger && matchesIp && matchesSource && matchesMessage) {
				if (thread != null && !thread.isEmpty())
					threads.add(thread);
			}
			if (matchesLevel && matchesThread && matchesIp && matchesSource && matchesMessage) {
				if (logger != null && !logger.isEmpty())
					loggers.add(logger);
			}
			if (matchesLevel && matchesThread && matchesLogger && matchesSource && matchesMessage) {
				if (entry.ip() != null && !entry.ip().isEmpty())
					ips.add(entry.ip());
			}
			if (matchesLevel && matchesThread && matchesLogger && matchesIp && matchesMessage) {
				if (entry.sourceFile() != null && !entry.sourceFile().isEmpty())
					sources.add(entry.sourceFile());
			}
		}

		levelFilter.setOptions(levels);
		threadFilter.setOptions(threads);
		loggerFilter.setOptions(loggers);
		ipFilter.setOptions(ips);
		sourceFilter.setOptions(sources);
	}

	public void applyFilters() {
		if (isUpdating)
			return;

		isUpdating = true;
		try {
			List<RowFilter<LogTableModel, Integer>> filters = new ArrayList<>();

			if (levelFilter.isActive()) {
				Set<String> selected = levelFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
						Object val = entry.getValue(LogColumn.LEVEL.getIndex());
						return val != null && selected.contains(val.toString());
					}
				});
			}

			if (threadFilter.isActive()) {
				Set<String> selected = threadFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
						Object val = entry.getValue(LogColumn.THREAD.getIndex());
						return val != null && selected.contains(val.toString());
					}
				});
			}

			if (loggerFilter.isActive()) {
				Set<String> selected = loggerFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
						Object val = entry.getValue(LogColumn.LOGGER.getIndex());
						return val != null && selected.contains(val.toString());
					}
				});
			}

			if (ipFilter.isActive()) {
				Set<String> selected = ipFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
						Object val = entry.getValue(LogColumn.IP.getIndex());
						return val != null && selected.contains(val.toString());
					}
				});
			}

			if (sourceFilter.isActive()) {
				Set<String> selected = sourceFilter.getSelectedOptions();
				filters.add(new RowFilter<>() {
					@Override
					public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
						Object val = entry.getValue(LogColumn.SOURCE.getIndex());
						return val != null && selected.contains(val.toString());
					}
				});
			}

			String msgText = messageFilterField.getText();
			if (!msgText.isEmpty()) {
				try {
					filters.add(RowFilter.regexFilter(msgText, LogColumn.MESSAGE.getIndex()));
				} catch (java.util.regex.PatternSyntaxException e) {
					// Ignore invalid regex
				}
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

	@Override
	protected JComponent getComponentForColumn(int modelIndex) {
		LogColumn col = LogColumn.fromIndex(modelIndex);
		if (col == null)
			return new JPanel();

		return switch (col) {
		case TIMESTAMP -> createHeaderLabel("Timestamp");
		case LEVEL -> levelFilter;
		case THREAD -> threadFilter;
		case LOGGER -> loggerFilter;
		case IP -> ipFilter;
		case MESSAGE -> {
			if (messageFilterField.isVisible()) {
				yield messageFilterField;
			} else {
				JButton btn = new JButton("Message");
				styleAsHeaderButton(btn);
				if (!messageFilterField.getText().isEmpty()) {
					btn.setIcon(new FilterIcon(Color.GRAY));
				}
				btn.addActionListener(e -> toggleMessageFilter(true));
				yield btn;
			}
		}
		case SOURCE -> sourceFilter;
		};
	}

	private void toggleMessageFilter(boolean showField) {
		messageFilterField.setVisible(showField);
		updateAlignment();
		if (showField) {
			SwingUtilities.invokeLater(() -> messageFilterField.requestFocusInWindow());
		}
	}

	public JTextField getMessageFilterField() {
		return messageFilterField;
	}
}
