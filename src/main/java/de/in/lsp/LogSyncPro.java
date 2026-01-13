package de.in.lsp;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import de.in.lsp.service.LogFileService;
import de.in.lsp.ui.AppActions;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.MemoryStatusBar;
import de.in.lsp.ui.ViewManager;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application class for LogSyncPro.
 * Manages the main window shell and delegates logic to specialized components.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogSyncPro extends JFrame implements LogView.LogViewListener {

    private final LogFileService logFileService = new LogFileService();
    private ViewManager viewManager;
    private AppActions appActions;

    private JPanel centerPanel;
    private MemoryStatusBar statusBar;
    private JMenu logFileMenu;
    private JMenuItem mergeItem;
    private JMenuItem closeSelectedItem;
    private final Map<Integer, Boolean> columnVisibility = new HashMap<>();

    private static final String[] COLUMN_NAMES = { "Timestamp", "Level", "Thread", "Logger", "Message", "Source" };

    public LogSyncPro(String[] args) {
        setTitle("LogSyncPro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));

        setupIcon();
        initColumnVisibility();
        setupUI();
        setupMenuBar();

        handleArguments(args);
    }

    private void initColumnVisibility() {
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            columnVisibility.put(i, true);
        }
    }

    private void setupIcon() {
        try {
            FlatSVGIcon icon = new FlatSVGIcon("icons/logo.svg", 32, 32);
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // Silently fail if icon can't be loaded
        }
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open (Multi)");
        openItem.addActionListener(e -> openLogs());
        fileMenu.add(openItem);

        JMenuItem importK8sItem = new JMenuItem("Import from K8s via SSH...");
        importK8sItem.addActionListener(e -> appActions.importFromK8s(this, columnVisibility));
        fileMenu.add(importK8sItem);

        mergeItem = new JMenuItem("Merge Selected Views");
        mergeItem.addActionListener(e -> appActions.mergeLogs(this, columnVisibility));
        mergeItem.setEnabled(false);
        fileMenu.add(mergeItem);

        closeSelectedItem = new JMenuItem("Close Selected Views");
        closeSelectedItem.addActionListener(e -> appActions.closeSelectedViews());
        closeSelectedItem.setEnabled(false);
        fileMenu.add(closeSelectedItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        logFileMenu = new JMenu("Logfile");
        logFileMenu.setEnabled(false);
        menuBar.add(logFileMenu);

        JMenu settingsMenu = new JMenu("Settings");
        JMenu columnsMenu = new JMenu("Columns");
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            final int colIndex = i;
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(COLUMN_NAMES[i], true);
            item.addActionListener(e -> toggleColumnVisibility(colIndex, item.isSelected()));
            columnsMenu.add(item);
        }
        settingsMenu.add(columnsMenu);
        menuBar.add(settingsMenu);

        menuBar.add(Box.createHorizontalGlue());

        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem quickGuideItem = new JMenuItem("Kurzanleitung");
        quickGuideItem.addActionListener(e -> appActions.openQuickGuide());
        helpMenu.add(quickGuideItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void toggleColumnVisibility(int colIndex, boolean visible) {
        columnVisibility.put(colIndex, visible);
        for (LogView view : viewManager.getLogViews()) {
            view.setColumnVisibility(colIndex, visible);
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        centerPanel = new JPanel(new BorderLayout()) {
            private FlatSVGIcon watermark;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (viewManager != null && viewManager.getCurrentRootComponent() != null)
                    return;

                if (watermark == null) {
                    try {
                        watermark = new FlatSVGIcon("icons/logo.svg", 256, 256);
                        watermark.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.GRAY));
                    } catch (Exception e) {
                        return;
                    }
                }

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));

                int x = (getWidth() - watermark.getIconWidth()) / 2;
                int y = (getHeight() - watermark.getIconHeight()) / 2;
                watermark.paintIcon(this, g2, x, y);
                g2.dispose();
            }
        };
        add(centerPanel, BorderLayout.CENTER);

        statusBar = new MemoryStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        viewManager = new ViewManager(centerPanel, this::onViewFocusChanged, this::updateLogFileMenu);
        appActions = new AppActions(this, viewManager);

        setupDragAndDrop();
        setupShortcuts();
    }

    private void onViewFocusChanged(LogView focusedView) {
        // Any additional logic needed when focus changes globally can go here
    }

    private void setupShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke("control ADD"), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                viewManager.setFontSize(Math.max(6, Math.min(48, viewManager.getFontSize() + 1)));
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                viewManager.setFontSize(Math.max(6, Math.min(48, viewManager.getFontSize() - 1)));
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control W"), "closeFocusedView");
        actionMap.put("closeFocusedView", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                appActions.closeFocusedLogView();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control shift F4"), "closeAllViews");
        actionMap.put("closeAllViews", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                appActions.closeAllLogViews();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control F"), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                appActions.openSearchDialog();
            }
        });
    }

    private void setupDragAndDrop() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support))
                    return false;
                try {
                    Transferable t = support.getTransferable();
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    backgroundLoadFiles(files);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    private void backgroundLoadFiles(List<File> files) {
        logFileService.backgroundLoadFiles(files,
                status -> statusBar.setStatus(status, status.contains("Loading")),
                (appName, group) -> {
                    String title = appName;
                    if (group.getFileCount() > 1) {
                        title += " (Auto-Merged)";
                    }
                    final String finalTitle = title;
                    SwingUtilities.invokeLater(
                            () -> viewManager.addLogView(group.getEntries(), finalTitle, columnVisibility, this));
                });
    }

    private void handleArguments(String[] args) {
        List<File> filesToOpen = new ArrayList<>();
        String sshValue = null;
        String fetchValue = null;

        for (String arg : args) {
            if (arg.startsWith("--open=")) {
                String pathsStr = arg.substring("--open=".length());
                String[] paths = pathsStr.split(",");
                for (String p : paths) {
                    filesToOpen.add(new File(p));
                }
            } else if (arg.startsWith("--ssh=")) {
                sshValue = arg.substring("--ssh=".length());
            } else if (arg.startsWith("--fetch=")) {
                fetchValue = arg.substring("--fetch=".length());
            }
        }

        if (sshValue != null) {
            appActions.importFromK8sAutomated(sshValue, fetchValue, this, columnVisibility);
        }

        if (!filesToOpen.isEmpty()) {
            backgroundLoadFiles(filesToOpen);
        }
    }

    private void openLogs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            backgroundLoadFiles(List.of(chooser.getSelectedFiles()));
        }
    }

    private void updateLogFileMenu() {
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

    @Override
    public void onClose(LogView view) {
        viewManager.removeView(view);
    }

    @Override
    public void onMinimize(LogView view) {
        viewManager.minimizeView(view);
    }

    @Override
    public void onMaximize(LogView view) {
        viewManager.rebuildLayout();
        updateLogFileMenu();
    }

    @Override
    public void onFocusGained(LogView view) {
        viewManager.updateFocusedView(view);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            LogSyncPro app = new LogSyncPro(args);
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}
