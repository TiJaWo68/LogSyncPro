package de.in.lsp.ui.helper;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogDetailView;
import de.in.lsp.ui.LogViewListener;

/**
 * Manages the detail view panel, including its display in the split pane or as a detached dialog.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class DetailViewManager {

	private final Component parent;
	private final JSplitPane splitPane;
	private final LogViewListener listener;

	private LogDetailView detailView;
	private JDialog detachedDialog;

	public DetailViewManager(Component parent, JSplitPane splitPane, LogViewListener listener) {
		this.parent = parent;
		this.splitPane = splitPane;
		this.listener = listener;
		createDetailView();
	}

	private void createDetailView() {
		detailView = new LogDetailView(this::detachDetailView, () -> toggleDetailView(false));
	}

	public void setEntry(LogEntry entry) {
		detailView.setEntry(entry);
	}

	public void setFontSize(int fontSize) {
		if (detailView != null) {
			detailView.setFontSize(fontSize);
		}
	}

	private void detachDetailView() {
		int width = 800;
		int height = 600;

		GraphicsConfiguration gc = parent.getGraphicsConfiguration();
		if (gc != null) {
			Rectangle bounds = gc.getBounds();
			width = Math.min(bounds.width - 100, 1000);
			height = Math.min(bounds.height - 100, 600);
		}

		if (splitPane.getBottomComponent() == detailView) {
			splitPane.setBottomComponent(null);
		}

		if (detachedDialog == null) {
			detachedDialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Log Details", java.awt.Dialog.ModalityType.MODELESS);
			detachedDialog.setLayout(new BorderLayout());
			detachedDialog.add(detailView);
			detachedDialog.setSize(width, height);
			detachedDialog.setLocationRelativeTo(parent);
			detachedDialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					reattachDetailView();
				}
			});

			setupDialogShortcuts();

		} else {
			detachedDialog.getContentPane().add(detailView);
		}

		detachedDialog.setVisible(true);
	}

	private void setupDialogShortcuts() {
		javax.swing.JRootPane rootPane = detachedDialog.getRootPane();
		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		javax.swing.ActionMap actionMap = rootPane.getActionMap();

		inputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "zoomIn");
		inputMap.put(KeyStroke.getKeyStroke("control ADD"), "zoomIn");
		inputMap.put(KeyStroke.getKeyStroke("control PLUS"), "zoomIn");
		actionMap.put("zoomIn", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.onIncreaseFontSize();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "zoomOut");
		inputMap.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
		actionMap.put("zoomOut", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.onDecreaseFontSize();
			}
		});
	}

	private void reattachDetailView() {
		if (detachedDialog != null) {
			detachedDialog.dispose();
			detachedDialog = null;
		}
		toggleDetailView(true);
	}

	public void toggleDetailView(boolean show) {
		if (show) {
			if (detachedDialog != null && detachedDialog.isVisible()) {
				// Already detached and visible, update content is handled by caller
				return;
			}
			if (splitPane.getBottomComponent() == null) {
				splitPane.setBottomComponent(detailView);
				splitPane.setDividerLocation(0.7);
			}
		} else {
			if (detachedDialog != null) {
				detachedDialog.dispose();
				detachedDialog = null;
			}
			if (splitPane.getBottomComponent() != null) {
				splitPane.setBottomComponent(null);
			}
		}
	}

	public void close() {
		if (detachedDialog != null) {
			detachedDialog.dispose();
		}
	}

	public LogDetailView getView() {
		return detailView;
	}
}
