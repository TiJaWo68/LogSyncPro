package de.in.lsp.ui;

import de.in.lsp.model.LogEntry;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles application-wide actions triggered by the menu or shortcuts.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class AppActions {
    private final JFrame parentFrame;
    private final ViewManager viewManager;
    private SearchDialog searchDialog;

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
                JOptionPane.showMessageDialog(parentFrame,
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
                    JOptionPane.showMessageDialog(parentFrame, "Quick guide not found!", "Error",
                            JOptionPane.ERROR_MESSAGE);
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
}
