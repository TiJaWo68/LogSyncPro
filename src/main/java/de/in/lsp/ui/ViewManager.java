package de.in.lsp.ui;

import de.in.lsp.model.LogEntry;
import javax.swing.*;
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
    private final JDesktopPane desktopPane;
    private final Consumer<LogView> focusCallback;
    private final Runnable layoutChangeCallback;

    private LogView focusedLogView;
    private int fontSize = 12;

    public ViewManager(JDesktopPane desktopPane, Consumer<LogView> focusCallback, Runnable layoutChangeCallback) {
        this.desktopPane = desktopPane;
        this.focusCallback = focusCallback;
        this.layoutChangeCallback = layoutChangeCallback;

        // Listen for desktop pane resizes to re-tile
        desktopPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                rebuildLayout();
            }
        });
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
            LogView.LogViewListener listener, LogView.ViewType viewType) {
        return addLogView(entries, title, columnVisibility, listener, viewType, null, null);
    }

    public LogView addLogView(List<LogEntry> entries, String title, Map<Integer, Boolean> columnVisibility,
            LogView.LogViewListener listener, LogView.ViewType viewType, String appName, String clientIp) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> addLogView(entries, title, columnVisibility, listener, viewType, appName, clientIp));
            return null;
        }
        LogView logView = new LogView(new ArrayList<>(entries), title, this::syncOtherViews, listener, viewType);
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
        desktopPane.add(logView);
        logView.setVisible(true);

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
        desktopPane.remove(view);

        if (focusedLogView == view) {
            focusedLogView = logViews.isEmpty() ? null : logViews.get(0);
            if (focusedLogView != null) {
                updateFocusedView(focusedLogView);
            }
        }
        rebuildLayout();
        layoutChangeCallback.run();
    }

    public void minimizeView(LogView view) {
        try {
            view.setIcon(true);
            if (view.getDesktopIcon() != null) {
                view.getDesktopIcon().setVisible(false);
            }
        } catch (java.beans.PropertyVetoException e) {
            // Ignore
        }
        if (!minimizedViews.contains(view)) {
            minimizedViews.add(view);
        }
        rebuildLayout();
        layoutChangeCallback.run();
    }

    public void toggleViewMinimized(LogView view, boolean minimized) {
        try {
            view.setIcon(minimized);
            if (minimized && view.getDesktopIcon() != null) {
                view.getDesktopIcon().setVisible(false);
            }
        } catch (java.beans.PropertyVetoException e) {
            // Ignore
        }
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
        LogView maximized = logViews.stream().filter(LogView::isMaximized).findFirst().orElse(null);

        List<LogView> visibleViews = logViews.stream()
                .filter(v -> !minimizedViews.contains(v))
                .toList();

        if (maximized != null) {
            for (LogView v : visibleViews) {
                if (v == maximized) {
                    v.setBounds(0, 0, desktopPane.getWidth(), desktopPane.getHeight());
                    v.toFront();
                } else {
                    v.setBounds(0, 0, 0, 0); // Hide others
                }
            }
        } else {
            if (visibleViews.isEmpty())
                return;

            int count = visibleViews.size();
            int width = desktopPane.getWidth() / count;
            int height = desktopPane.getHeight();

            for (int i = 0; i < count; i++) {
                LogView v = visibleViews.get(i);
                v.setBounds(i * width, 0, width, height);
            }
        }

        desktopPane.revalidate();
        desktopPane.repaint();
    }

    public void updateFocusedView(LogView view) {
        if (focusedLogView != null && focusedLogView != view) {
            // No need to manually set focused false, JInternalFrame handles selection
        }
        focusedLogView = view;
        if (focusedLogView != null) {
            try {
                focusedLogView.setSelected(true);
            } catch (java.beans.PropertyVetoException e) {
                // Ignore
            }
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
                    title = "Remote(" + clientIp + "): " + entry.getSimpleLoggerName();
                }
                addLogView(initialEntries, title, columnVisibility, listener, LogView.ViewType.TCP, appName,
                        clientIp);
                pendingBuffers.remove(key);

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
        return desktopPane;
    }
}
