package de.in.lsp.ui.actions;

import de.in.lsp.util.LspLogger;
import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.SearchDialog;
import de.in.lsp.ui.ViewManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles view-related actions like merging, closing, and searching logs.
 * Orchestrates interaction between the MainFrame and the ViewManager.
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

    public void mergeLogs(LogView.LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
        List<LogView> selectedViews = viewManager.getLogViews().stream().filter(LogView::isViewSelected).toList();
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
        LspLogger.info("Merged " + selectedViews.size() + " views into a new view.");
        viewManager.addLogView(allEntries, "Merged View", columnVisibility, listener, LogView.ViewType.MERGED);
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
