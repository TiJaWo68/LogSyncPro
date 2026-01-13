package de.in.lsp.ui;

import de.in.lsp.model.LogEntry;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages LogView instances, their synchronization, and layout within the main
 * window.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ViewManager {
    private final List<LogView> logViews = new ArrayList<>();
    private final List<LogView> minimizedViews = new ArrayList<>();
    private final JPanel centerPanel;
    private final Consumer<LogView> focusCallback;
    private final Runnable layoutChangeCallback;

    private LogView focusedLogView;
    private JComponent currentRootComponent;
    private int fontSize = 12;

    public ViewManager(JPanel centerPanel, Consumer<LogView> focusCallback, Runnable layoutChangeCallback) {
        this.centerPanel = centerPanel;
        this.focusCallback = focusCallback;
        this.layoutChangeCallback = layoutChangeCallback;
    }

    public List<LogView> getLogViews() {
        return logViews;
    }

    public List<LogView> getMinimizedViews() {
        return minimizedViews;
    }

    public LogView getFocusedLogView() {
        return focusedLogView;
    }

    public void addLogView(List<LogEntry> entries, String title, Map<Integer, Boolean> columnVisibility,
            LogView.LogViewListener listener) {
        LogView logView = new LogView(entries, title, this::syncOtherViews, listener);

        columnVisibility.forEach((index, visible) -> {
            if (!visible) {
                logView.setColumnVisibility(index, false);
            }
        });

        logViews.add(logView);
        logView.updateFontSize(fontSize);

        if (logViews.size() == 1) {
            updateFocusedView(logView);
        }

        rebuildLayout();
        layoutChangeCallback.run();
    }

    public void removeView(LogView view) {
        logViews.remove(view);
        minimizedViews.remove(view);
        if (focusedLogView == view) {
            focusedLogView = logViews.isEmpty() ? null : logViews.get(0);
            if (focusedLogView != null) {
                focusedLogView.setFocused(true);
            }
        }
        rebuildLayout();
        layoutChangeCallback.run();
    }

    public void minimizeView(LogView view) {
        if (!minimizedViews.contains(view)) {
            minimizedViews.add(view);
        }
        rebuildLayout();
        layoutChangeCallback.run();
    }

    public void toggleViewMinimized(LogView view, boolean minimized) {
        if (minimized) {
            if (!minimizedViews.contains(view))
                minimizedViews.add(view);
        } else {
            minimizedViews.remove(view);
        }
        rebuildLayout();
        layoutChangeCallback.run();
    }

    public void rebuildLayout() {
        centerPanel.removeAll();
        currentRootComponent = null;

        LogView maximized = logViews.stream().filter(LogView::isMaximized).findFirst().orElse(null);
        if (maximized != null) {
            centerPanel.add(maximized, BorderLayout.CENTER);
            currentRootComponent = maximized;
        } else {
            List<LogView> visibleViews = logViews.stream()
                    .filter(v -> !minimizedViews.contains(v))
                    .toList();

            for (LogView view : visibleViews) {
                if (currentRootComponent == null) {
                    currentRootComponent = view;
                } else {
                    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, currentRootComponent, view);
                    split.setResizeWeight(0.5);
                    split.setContinuousLayout(true);
                    currentRootComponent = split;
                }
            }

            if (currentRootComponent != null) {
                centerPanel.add(currentRootComponent, BorderLayout.CENTER);
            }
        }

        centerPanel.revalidate();
        centerPanel.repaint();
    }

    public void updateFocusedView(LogView view) {
        if (focusedLogView != null && focusedLogView != view) {
            focusedLogView.setFocused(false);
        }
        focusedLogView = view;
        if (focusedLogView != null) {
            focusedLogView.setFocused(true);
        }
        focusCallback.accept(focusedLogView);
    }

    private void syncOtherViews(LogView sourceView, LocalDateTime timestamp) {
        for (LogView view : logViews) {
            if (view != sourceView) {
                view.scrollToTimestamp(timestamp);
            }
        }
    }

    public void setFontSize(int size) {
        this.fontSize = size;
        for (LogView view : logViews) {
            view.updateFontSize(fontSize);
        }
    }

    public int getFontSize() {
        return fontSize;
    }

    public JComponent getCurrentRootComponent() {
        return currentRootComponent;
    }
}
