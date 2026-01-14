package de.in.lsp.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.KeyboardFocusManager;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.cuberact.swing.layout.Cell;

import de.in.lsp.model.LogEntry;

/**
 * Handles application-wide actions triggered by the menu or shortcuts.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class AppActions {
	private final JFrame parentFrame;
	private final ViewManager viewManager;
	private SearchDialog searchDialog;

	private static final String PREF_LAST_HOSTNAME = "last_hostname";

	public AppActions(JFrame parentFrame, ViewManager viewManager) {
		this.parentFrame = parentFrame;
		this.viewManager = viewManager;
	}

	public void mergeLogs(LogView.LogViewListener listener, java.util.Map<Integer, Boolean> columnVisibility) {
		List<LogView> selectedViews = viewManager.getLogViews().stream().filter(LogView::isSelected).toList();
		if (selectedViews.isEmpty()) {
			JOptionPane.showMessageDialog(parentFrame, "No log views selected.");
			return;
		}

		for (LogView view : selectedViews) {
			if (!view.hasTimestamps()) {
				JOptionPane.showMessageDialog(parentFrame, "Merging is not possible because one of the selected views (" + view.getTitle()
						+ ") contains entries without timestamps.", "Merge Failed", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		List<LogEntry> allEntries = new ArrayList<>();
		for (LogView view : selectedViews) {
			allEntries.addAll(view.getEntries());
			viewManager.toggleViewMinimized(view, true);
		}
		Collections.sort(allEntries);
		viewManager.addLogView(allEntries, "Merged View", columnVisibility, listener);
	}

	public void closeSelectedViews() {
		List<LogView> toRemove = viewManager.getLogViews().stream().filter(LogView::isSelected).toList();
		if (toRemove.isEmpty()) {
			JOptionPane.showMessageDialog(parentFrame, "No log views selected.");
			return;
		}
		for (LogView view : toRemove) {
			viewManager.removeView(view);
		}
	}

	public void closeAllLogViews() {
		if (viewManager.getLogViews().isEmpty())
			return;

		List<LogView> all = new ArrayList<>(viewManager.getLogViews());
		for (LogView v : all) {
			viewManager.removeView(v);
		}

		if (searchDialog != null && searchDialog.isVisible()) {
			searchDialog.dispose();
		}
	}

	public void openQuickGuide() {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				java.net.URL resourceUrl = getClass().getResource("/quick_guide.html");
				if (resourceUrl != null) {
					if ("file".equals(resourceUrl.getProtocol())) {
						Desktop.getDesktop().browse(resourceUrl.toURI());
					} else {
						InputStream in = getClass().getResourceAsStream("/quick_guide.html");
						File tempFile = File.createTempFile("quick_guide", ".html");
						tempFile.deleteOnExit();
						Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						Desktop.getDesktop().browse(tempFile.toURI());
					}
				} else {
					JOptionPane.showMessageDialog(parentFrame, "Quick guide not found!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(parentFrame, "Could not open quick guide: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void openSearchDialog() {
		if (viewManager.getLogViews().isEmpty())
			return;

		LogView current = viewManager.getFocusedLogView();
		if (current == null) {
			current = viewManager.getLogViews().get(0);
		}

		if (searchDialog == null || !searchDialog.isVisible()) {
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

	public void closeFocusedLogView() {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner == null)
			return;

		Component parent = focusOwner;
		while (parent != null && !(parent instanceof LogView)) {
			parent = parent.getParent();
		}

		if (parent instanceof LogView) {
			viewManager.removeView((LogView) parent);
		} else if (viewManager.getFocusedLogView() != null) {
			viewManager.removeView(viewManager.getFocusedLogView());
		}
	}

	private String getLastHostname() {
		return Preferences.userNodeForPackage(AppActions.class).get(PREF_LAST_HOSTNAME, "");
	}

	private void saveLastHostname(String host) {
		if (host != null && !host.isEmpty()) {
			Preferences.userNodeForPackage(AppActions.class).put(PREF_LAST_HOSTNAME, host);
		}
	}

	public void importFromK8s(LogView.LogViewListener listener, java.util.Map<Integer, Boolean> columnVisibility) {
		org.cuberact.swing.layout.Composite panel = new org.cuberact.swing.layout.Composite();
		JTextField hostField = new JTextField(getLastHostname(), 20);
		JTextField userField = new JTextField("node-admin", 20);
		JPasswordField passField = new JPasswordField("", 20);

		panel.addCell(new JLabel("Host: ")).align(Cell.RIGHT);
		panel.addCell(hostField).width(400).rowEnd(true).padBottom(5);
		panel.addCell(new JLabel("User: ")).align(Cell.RIGHT);
		panel.addCell(userField).width(400).rowEnd(true).padBottom(5);
		panel.addCell(new JLabel("Password: ")).align(Cell.RIGHT);
		panel.addCell(passField).width(400);

		int result = JOptionPane.showConfirmDialog(parentFrame, panel, "SSH Connection", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
			return;

		performSshDiscovery(hostField.getText(), 22, userField.getText(), new String(passField.getPassword()), null, listener,
				columnVisibility);
	}

	public void importFromK8sAutomated(String credentials, String fetchFilter, LogView.LogViewListener listener,
			java.util.Map<Integer, Boolean> columnVisibility) {
		String host = "";
		String user = "node-admin";
		String password = "";

		// Format: [user[:password]@]host
		int atIndex = credentials.lastIndexOf('@');
		if (atIndex != -1) {
			host = credentials.substring(atIndex + 1);
			String userPass = credentials.substring(0, atIndex);
			int colonIndex = userPass.indexOf(':');
			if (colonIndex != -1) {
				user = userPass.substring(0, colonIndex);
				password = userPass.substring(colonIndex + 1);
			} else {
				user = userPass;
			}
		} else {
			host = credentials;
		}

		if (password.isEmpty() || host.isEmpty()) {
			showSshDialog(host, user, password, fetchFilter, listener, columnVisibility);
		} else {
			performSshDiscovery(host, 22, user, password, fetchFilter, listener, columnVisibility);
		}
	}

	private void showSshDialog(String host, String user, String password, String fetchFilter, LogView.LogViewListener listener,
			java.util.Map<Integer, Boolean> columnVisibility) {
		org.cuberact.swing.layout.Composite panel = new org.cuberact.swing.layout.Composite();
		JTextField hostField = new JTextField(host.isEmpty() ? getLastHostname() : host, 30);
		JTextField userField = new JTextField(user, 30);
		JPasswordField passField = new JPasswordField(password, 30);

		panel.addCell(new JLabel("Host: ")).align(2);
		panel.addCell(hostField).width(400).rowEnd(true);
		panel.addCell(new JLabel("User: ")).align(2);
		panel.addCell(userField).width(400).rowEnd(true);
		panel.addCell(new JLabel("Password: ")).align(2);
		panel.addCell(passField).width(400);

		int result = JOptionPane.showConfirmDialog(parentFrame, panel, "SSH Connection", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
			return;

		performSshDiscovery(hostField.getText(), 22, userField.getText(), new String(passField.getPassword()), fetchFilter, listener,
				columnVisibility);
	}

	private void performSshDiscovery(String host, int port, String user, String password, String fetchFilter,
			LogView.LogViewListener listener, java.util.Map<Integer, Boolean> columnVisibility) {
		saveLastHostname(host);
		ProgressMonitor pm = new ProgressMonitor(parentFrame, "Connecting and discovering pods...", "", 0, 100);
		new Thread(() -> {
			try (de.in.lsp.service.SshK8sService sshService = new de.in.lsp.service.SshK8sService()) {
				sshService.connect(host, port, user, password);
				pm.setNote("Fetching pods...");
				var namespaces = sshService.discoverPods();

				final List<K8sPodSelectionDialog.SelectedContainer> selected = new ArrayList<>();
				if (fetchFilter != null && !fetchFilter.isEmpty()) {
					selected.addAll(autoSelectContainers(namespaces, fetchFilter));
				}

				if (selected.isEmpty()) {
					SwingUtilities.invokeAndWait(() -> {
						K8sPodSelectionDialog dialog = new K8sPodSelectionDialog(parentFrame, namespaces);
						dialog.setVisible(true);
						selected.addAll(dialog.getSelectedContainers());
					});
				}

				if (selected.isEmpty())
					return;

				for (var target : selected) {
					pm.setNote("Streaming logs from " + target.pod + "...");
					try (InputStream is = sshService.streamLogs(target.namespace, target.pod, target.container)) {
						de.in.lsp.manager.LogManager lm = new de.in.lsp.manager.LogManager();
						String sourceName = target.pod + "_" + target.container + ".log";
						List<LogEntry> entries = lm.parseStream(is, sourceName);
						SwingUtilities.invokeLater(() -> {
							viewManager.addLogView(entries, sourceName, columnVisibility, listener);
						});
					} catch (Exception ex) {
						ex.printStackTrace();
						SwingUtilities.invokeLater(
								() -> JOptionPane.showMessageDialog(parentFrame, "Error streaming " + target.pod + ": " + ex.getMessage()));
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentFrame, "K8s Discovery failed: " + ex.getMessage()));
			} finally {
				pm.close();
			}
		}).start();
	}

	private List<K8sPodSelectionDialog.SelectedContainer> autoSelectContainers(
			List<de.in.lsp.service.SshK8sService.K8sNamespace> namespaces, String filter) {
		List<K8sPodSelectionDialog.SelectedContainer> result = new ArrayList<>();
		java.util.Set<String> seen = new java.util.HashSet<>();
		String[] patterns = filter.split(",");
		for (String pattern : patterns) {
			String p = pattern.trim();
			for (var ns : namespaces) {
				for (var pod : ns.getPods()) {
					for (var container : pod.getContainers()) {
						String fullPath = ns.getName() + "/" + pod.getName() + "/" + container;
						if (matches(fullPath, p) && seen.add(fullPath)) {
							result.add(new K8sPodSelectionDialog.SelectedContainer(ns.getName(), pod.getName(), container));
						}
					}
				}
			}
		}
		return result;
	}

	private boolean matches(String text, String pattern) {
		if (!pattern.contains("/")) {
			return text.contains(pattern);
		}
		String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
		return text.matches(regex);
	}
}
