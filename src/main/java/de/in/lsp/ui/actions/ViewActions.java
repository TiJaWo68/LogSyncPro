package de.in.lsp.ui.actions;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.LogViewListener;
import de.in.lsp.ui.SearchDialog;
import de.in.lsp.ui.ViewManager;
import de.in.lsp.ui.ViewType;
import de.in.lsp.util.LspLogger;

/**
 * Handles view-related actions like merging, closing, and searching logs.
 * Orchestrates interaction between the MainFrame and the
 * ViewManager.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ViewActions {
	private final JFrame parentFrame;
	private final ViewManager viewManager;
	private SearchDialog searchDialog;

	public ViewActions(JFrame parentFrame, ViewManager viewManager) {
		this.parentFrame = parentFrame;
		this.viewManager = viewManager;
	}

	public void mergeLogs(LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
		List<LogView> selectedViews = viewManager.getLogViews().stream().filter(LogView::isViewSelected).toList();
		if (selectedViews.isEmpty()) {
			JOptionPane.showMessageDialog(parentFrame, "No log views selected.");
			return;
		}

		for (LogView view : selectedViews) {
			if (!view.hasTimestamps()) {
				JOptionPane
						.showMessageDialog(parentFrame,
								"Merging is not possible because one of the selected views (" + view.getTitle()
										+ ") contains entries without timestamps.",
								"Merge Failed", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		List<LogEntry> allEntries = new ArrayList<>();
		for (LogView view : selectedViews) {
			allEntries.addAll(view.getEntries());
			viewManager.toggleViewMinimized(view, true);
		}
		Collections.sort(allEntries);
		LspLogger.info("Merged " + selectedViews.size() + " views into a new view.");
		viewManager.addLogView(allEntries, "Merged View", columnVisibility, listener, ViewType.MERGED);
	}

	public void closeSelectedViews() {
		List<LogView> toRemove = viewManager.getLogViews().stream().filter(LogView::isViewSelected).toList();
		if (toRemove.isEmpty()) {
			JOptionPane.showMessageDialog(parentFrame, "No log views selected.");
			return;
		}
		LspLogger.info("Closing " + toRemove.size() + " selected log views.");
		for (LogView view : toRemove) {
			viewManager.removeView(view);
		}
	}

	public void closeAllLogViews() {
		if (viewManager.getLogViews().isEmpty())
			return;

		List<LogView> all = new ArrayList<>(viewManager.getLogViews());
		LspLogger.info("Closing all " + all.size() + " log views.");
		for (LogView v : all) {
			viewManager.removeView(v);
		}

		if (searchDialog != null && searchDialog.isVisible()) {
			searchDialog.dispose();
		}
	}

	public void closeFocusedLogView() {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner == null)
			return;

		Component parent = focusOwner;
		while (parent != null && !(parent instanceof LogView)) {
			parent = parent.getParent();
		}

		if (parent instanceof LogView) {
			LspLogger.info("Closing focused log view: " + ((LogView) parent).getTitle());
			viewManager.removeView((LogView) parent);
		} else if (viewManager.getFocusedLogView() != null) {
			LspLogger.info("Closing focused log view: " + viewManager.getFocusedLogView().getTitle());
			viewManager.removeView(viewManager.getFocusedLogView());
		}
	}

	/**
	 * Exports log views: If multiple views are selected, exports as a ZIP archive.
	 * If only one view is available
	 * (selected or focused), exports as a single .log file. Uses a JFileChooser for
	 * the target location.
	 */
	public void exportSelectedViews() {
		List<LogView> selectedViews = viewManager.getLogViews().stream().filter(LogView::isViewSelected).toList();

		// Fallback to focused view if none selected
		if (selectedViews.isEmpty()) {
			LogView focused = viewManager.getFocusedLogView();
			if (focused != null) {
				selectedViews = List.of(focused);
			} else {
				JOptionPane.showMessageDialog(parentFrame, "No log views selected or focused.");
				return;
			}
		}

		boolean multipleViews = selectedViews.size() > 1;
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Export " + (multipleViews ? selectedViews.size() + " Views" : "View"));

		if (multipleViews) {
			chooser.setFileFilter(new FileNameExtensionFilter("ZIP Archive (*.zip)", "zip"));
			chooser.setSelectedFile(new File("logs_export.zip"));
		} else {
			chooser.setFileFilter(new FileNameExtensionFilter("Log File (*.log)", "log"));
			String suggestedName = sanitizeFilename(selectedViews.get(0).getBaseTitle()) + ".log";
			chooser.setSelectedFile(new File(suggestedName));
		}

		if (chooser.showSaveDialog(parentFrame) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File target = chooser.getSelectedFile();
		try {
			if (multipleViews) {
				if (!target.getName().toLowerCase().endsWith(".zip")) {
					target = new File(target.getAbsolutePath() + ".zip");
				}
				exportAsZip(selectedViews, target);
			} else {
				if (!target.getName().toLowerCase().endsWith(".log")) {
					target = new File(target.getAbsolutePath() + ".log");
				}
				exportSingleView(selectedViews.get(0), target);
			}
			LspLogger.info("Exported " + selectedViews.size() + " view(s) to " + target.getAbsolutePath());
			JOptionPane.showMessageDialog(parentFrame, "Export successful:\n" + target.getAbsolutePath(),
					"Export Complete", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException ex) {
			LspLogger.error("Export failed: " + ex.getMessage(), ex);
			JOptionPane.showMessageDialog(parentFrame, "Export failed: " + ex.getMessage(), "Export Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void exportSingleView(LogView view, File target) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8))) {
			for (LogEntry entry : view.getEntries()) {
				writer.write(entry.rawLine());
				writer.newLine();
			}
		}
	}

	private void exportAsZip(List<LogView> views, File target) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target), StandardCharsets.UTF_8)) {
			for (LogView view : views) {
				String entryName = sanitizeFilename(view.getBaseTitle()) + ".log";
				zos.putNextEntry(new ZipEntry(entryName));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
				for (LogEntry entry : view.getEntries()) {
					writer.write(entry.rawLine());
					writer.newLine();
				}
				writer.flush();
				zos.closeEntry();
			}
		}
	}

	private String sanitizeFilename(String name) {
		return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
	}

	public void openSearchDialog() {
		if (viewManager.getLogViews().isEmpty())
			return;

		LogView current = viewManager.getFocusedLogView();
		if (current == null) {
			current = viewManager.getLogViews().get(0);
		}

		if (searchDialog == null || !searchDialog.isVisible()) {
			LspLogger.info("Opening search dialog for view: " + current.getTitle());
			searchDialog = new SearchDialog(parentFrame, viewManager.getLogViews(), current);
			searchDialog.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosed(java.awt.event.WindowEvent e) {
					searchDialog = null;
				}
			});
			searchDialog.setVisible(true);
		} else {
			searchDialog.setCurrentView(current);
			searchDialog.toFront();
			searchDialog.requestFocus();
		}
	}
}
