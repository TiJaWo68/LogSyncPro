package de.in.lsp.ui;

import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.in.lsp.LogSyncPro;
import de.in.lsp.service.LogStreamServer;
import de.in.lsp.service.ReceiverManager;
import de.in.lsp.service.UpdateService;
import de.in.lsp.ui.actions.HelpActions;
import de.in.lsp.ui.actions.RemoteActions;
import de.in.lsp.ui.actions.ViewActions;
import de.in.lsp.ui.dialog.LoggingSettingsDialog;
import de.in.lsp.util.LspLogger;

/**
 * Manages the Main Menu Bar for LogSyncPro.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogSyncProMenu {

    private final LogSyncPro mainFrame;
    private final ViewActions viewActions;
    private final RemoteActions remoteActions;
    private final HelpActions helpActions;
    private final ReceiverManager receiverManager;
    private final ViewManager viewManager;
    private final Map<Integer, Boolean> columnVisibility;
    private final UpdateService updateService;
    private final Runnable openLogsAction;
    private final Runnable exitAction;

    private JMenu logFileMenu;
    private JMenuItem mergeItem;
    private JMenuItem closeSelectedItem;

    private final String[] COLUMN_NAMES = { "Timestamp", "Level", "Thread", "Logger", "IP", "Message", "Source" };

    public LogSyncProMenu(LogSyncPro mainFrame, ViewActions viewActions, RemoteActions remoteActions,
            HelpActions helpActions, ReceiverManager receiverManager, ViewManager viewManager,
            Map<Integer, Boolean> columnVisibility, UpdateService updateService, Runnable openLogsAction,
            Runnable exitAction) {
        this.mainFrame = mainFrame;
        this.viewActions = viewActions;
        this.remoteActions = remoteActions;
        this.helpActions = helpActions;
        this.receiverManager = receiverManager;
        this.viewManager = viewManager;
        this.columnVisibility = columnVisibility;
        this.updateService = updateService;
        this.openLogsAction = openLogsAction;
        this.exitAction = exitAction;
    }

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open (Multi)");
        openItem.addActionListener(e -> openLogsAction.run());
        fileMenu.add(openItem);

        JMenuItem importK8sItem = new JMenuItem("Import from K8s via SSH...");
        importK8sItem.addActionListener(e -> remoteActions.importFromK8s(mainFrame, columnVisibility));
        fileMenu.add(importK8sItem);

        mergeItem = new JMenuItem("Merge Selected Views");
        mergeItem.addActionListener(e -> viewActions.mergeLogs(mainFrame, columnVisibility));
        mergeItem.setEnabled(false);
        fileMenu.add(mergeItem);

        closeSelectedItem = new JMenuItem("Close Selected Views");
        closeSelectedItem.addActionListener(e -> viewActions.closeSelectedViews());
        closeSelectedItem.setEnabled(false);
        fileMenu.add(closeSelectedItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> exitAction.run());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        logFileMenu = new JMenu("Logfile");
        logFileMenu.setEnabled(false);
        menuBar.add(logFileMenu);

        JMenu settingsMenu = new JMenu("Settings");

        JMenu receiverMenu = new JMenu("Receiver");
        for (LogStreamServer receiver : receiverManager.getReceivers()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(receiver.getProtocol() + " (" + receiver.getPort() + ")",
                    receiver.isRunning());
            item.addActionListener(e -> {
                try {
                    if (item.isSelected()) {
                        LspLogger.info("Starting receiver on port " + receiver.getPort());
                        receiverManager.startReceiver(receiver.getPort());
                    } else {
                        LspLogger.info("Stopping receiver on port " + receiver.getPort());
                        receiverManager.stopReceiver(receiver.getPort());
                    }
                } catch (Exception ex) {
                    item.setSelected(false);
                    LspLogger.error("Failed to start/stop receiver: " + ex.getMessage(), ex);
                    JOptionPane.showMessageDialog(mainFrame, "Failed to start receiver: " + ex.getMessage());
                }
            });
            receiverMenu.add(item);
        }
        settingsMenu.add(receiverMenu);
        settingsMenu.addSeparator();

        JMenu columnsMenu = new JMenu("Columns");
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            final int colIndex = i;
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(COLUMN_NAMES[i], true);
            item.addActionListener(e -> toggleColumnVisibility(colIndex, item.isSelected()));
            columnsMenu.add(item);
        }
        settingsMenu.add(columnsMenu);
        settingsMenu.addSeparator();

        JMenuItem loggingSettingsItem = new JMenuItem("Logging...");
        loggingSettingsItem.addActionListener(e -> new LoggingSettingsDialog(mainFrame).setVisible(true));
        settingsMenu.add(loggingSettingsItem);

        menuBar.add(settingsMenu);

        menuBar.add(Box.createHorizontalGlue());

        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem quickGuideItem = new JMenuItem("Kurzanleitung");
        quickGuideItem.addActionListener(e -> helpActions.openQuickGuide());
        helpMenu.add(quickGuideItem);

        JMenuItem updateItem = new JMenuItem("Update");
        updateItem.addActionListener(e -> updateService.checkForUpdatesAsync(true));
        helpMenu.add(updateItem);

        JMenuItem aboutItem = new JMenuItem("Über LogSyncPro");
        aboutItem.addActionListener(e -> helpActions.openAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private void toggleColumnVisibility(int colIndex, boolean visible) {
        LspLogger.info("Toggling column '" + COLUMN_NAMES[colIndex] + "' visibility to " + visible);
        columnVisibility.put(colIndex, visible);
        for (LogView view : viewManager.getLogViews()) {
            view.setColumnVisibility(colIndex, visible);
        }
    }

    public void updateLogFileMenu() {
        boolean hasLogs = !viewManager.getLogViews().isEmpty();
        logFileMenu.setEnabled(hasLogs);
        mergeItem.setEnabled(hasLogs);
        closeSelectedItem.setEnabled(hasLogs);

        logFileMenu.removeAll();
        for (LogView view : viewManager.getLogViews()) {
            boolean isVisible = !viewManager.getMinimizedViews().contains(view);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(view.getTitle(), isVisible);
            item.addActionListener(e -> viewManager.toggleViewMinimized(view, !item.isSelected()));
            logFileMenu.add(item);
        }
    }
}
