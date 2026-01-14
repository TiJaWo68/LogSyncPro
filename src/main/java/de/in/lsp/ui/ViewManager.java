package de.in.lsp.ui;

import de.in.lsp.model.LogEntry;
import javax.swing.*;
import java.awt.*;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages LogView instances, their synchronization, and layout within the main
 * window.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ViewManager {
    private final List<LogView> logViews = new CopyOnWriteArrayList<>();
    private final List<LogView> minimizedViews = new CopyOnWriteArrayList<>();
    private final Map<String, List<LogEntry>> pendingBuffers = new ConcurrentHashMap<>();
    private final Map<SocketAddress, LogView> activeStreams = new ConcurrentHashMap<>();
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

    public LogView addLogView(List<LogEntry> entries, String title, Map<Integer, Boolean> columnVisibility,
            LogView.LogViewListener listener) {
        return addLogView(entries, title, columnVisibility, listener, null, null);
    }

    public LogView addLogView(List<LogEntry> entries, String title, Map<Integer, Boolean> columnVisibility,
            LogView.LogViewListener listener, String appName, String clientIp) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addLogView(entries, title, columnVisibility, listener, appName, clientIp));
            return null; // Return null if not on EDT, but should be avoided for internal callers
        }
        LogView logView = new LogView(new ArrayList<>(entries), title, this::syncOtherViews, listener);
        logView.setMetaData(appName, clientIp);
        if (!entries.isEmpty() && entries.get(0).loggerName() != null) {
            logView.setInitialLoggerName(entries.get(0).loggerName());
        }

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
        return logView;
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

    public LogView findView(String appName, String clientIp) {
        return findView(appName, clientIp, null);
    }

    public LogView findView(String appName, String clientIp, String loggerName) {
        if (appName == null || clientIp == null)
            return null;
        return logViews.stream()
                .filter(v -> {
                    if (!appName.equals(v.getAppName()) || !clientIp.equals(v.getClientIp())) {
                        return false;
                    }
                    if ("RemoteApp".equals(appName) && loggerName != null) {
                        return loggerName.equals(v.getInitialLoggerName());
                    }
                    return true;
                })
                .findFirst()
                .orElse(null);
    }

    public void addEntry(LogView view, LogEntry entry) {
        SwingUtilities.invokeLater(() -> {
            view.addEntry(entry);
        });
    }

    public void handleStreamingEntry(LogEntry entry, SocketAddress remoteAddress, LogView.LogViewListener listener,
            Map<Integer, Boolean> columnVisibility) {
        if (entry == null) {
            // Connection closed
            activeStreams.remove(remoteAddress);
            return;
        }

        LogView existingBySession = activeStreams.get(remoteAddress);
        if (existingBySession != null) {
            addEntry(existingBySession, entry);
            return;
        }

        String appName = entry.sourceFile();
        String clientIp = entry.ip();
        String loggerName = entry.loggerName();

        // Use the robust findView with loggerName heuristic
        LogView existing = findView(appName, clientIp, loggerName);

        if (existing != null) {
            activeStreams.put(remoteAddress, existing);
            addEntry(existing, entry);
            return;
        }

        String key = appName + "|" + clientIp + "|" + loggerName;
        boolean[] isNew = { false };
        pendingBuffers.compute(key, (k, buffer) -> {
            if (buffer == null) {
                buffer = new CopyOnWriteArrayList<>();
                buffer.add(entry);
                isNew[0] = true;
                return buffer;
            } else {
                buffer.add(entry);
                return buffer;
            }
        });

        if (isNew[0]) {
            SwingUtilities.invokeLater(() -> {
                List<LogEntry> initialEntries = pendingBuffers.get(key);
                String title = appName;
                if (!"RemoteApp".equals(appName)) {
                    title += " (" + clientIp + ")";
                } else {
                    title = "Remote: " + entry.getSimpleLoggerName();
                }
                addLogView(initialEntries, title, columnVisibility, listener, appName, clientIp);
                pendingBuffers.remove(key);

                // Add to active streams after view is created (but we need the view instance)
                // Since addLogView is async, we'll find it again using the more specific lookup
                SwingUtilities.invokeLater(() -> {
                    LogView created = findView(appName, clientIp, loggerName);
                    if (created != null) {
                        activeStreams.put(remoteAddress, created);
                    }
                });
            });
        }
    }

    public JComponent getCurrentRootComponent() {
        return currentRootComponent;
    }
}
