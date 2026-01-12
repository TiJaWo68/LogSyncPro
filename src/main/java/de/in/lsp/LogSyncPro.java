package de.in.lsp;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import de.in.lsp.manager.LogManager;
import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.MemoryStatusBar;
import de.in.lsp.ui.SearchDialog;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main application class for LogSyncPro.
 * Manages the main window, menu bar, and dynamic splitting of log views.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogSyncPro extends JFrame implements LogView.LogViewListener {

    private final LogManager logManager = new LogManager();
    private final List<LogView> logViews = new ArrayList<>();
    private final List<LogView> minimizedViews = new ArrayList<>();
    private JPanel centerPanel;
    private JComponent currentRootComponent;
    private MemoryStatusBar statusBar;
    private JMenu logFileMenu;
    private JMenuItem mergeItem;
    private JMenuItem closeSelectedItem;
    private final Map<Integer, Boolean> columnVisibility = new HashMap<>();
    private int fontSize = 12;
    private LogView focusedLogView;

    private static final String[] COLUMN_NAMES = { "Timestamp", "Level", "Thread", "Logger", "Message", "Source" };

    public LogSyncPro(String[] args) {
        setTitle("LogSyncPro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));

        setupIcon();
        initColumnVisibility();
        setupMenuBar();
        setupUI();

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

        mergeItem = new JMenuItem("Merge Selected Views");
        mergeItem.addActionListener(e -> mergeLogs());
        mergeItem.setEnabled(false);
        fileMenu.add(mergeItem);

        closeSelectedItem = new JMenuItem("Close Selected Views");
        closeSelectedItem.addActionListener(e -> closeSelectedViews());
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
        quickGuideItem.addActionListener(e -> openQuickGuide());
        helpMenu.add(quickGuideItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void toggleColumnVisibility(int colIndex, boolean visible) {
        columnVisibility.put(colIndex, visible);
        for (LogView view : logViews) {
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
                if (currentRootComponent != null)
                    return; // Don't draw if logs are open

                if (watermark == null) {
                    try {
                        watermark = new FlatSVGIcon("icons/logo.svg", 256, 256);
                        // Set a color filter to make it monochrome (e.g., using current foreground
                        // color but fixed)
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

        setupDragAndDrop();
        setupShortcuts();
    }

    private void setupShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // Zoom In
        inputMap.put(KeyStroke.getKeyStroke("control EQUALS"), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke("control ADD"), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                changeFontSize(1);
            }
        });

        // Zoom Out
        inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                changeFontSize(-1);
            }
        });

        // Close Focused LogView (Ctrl+W)
        inputMap.put(KeyStroke.getKeyStroke("control W"), "closeFocusedView");
        actionMap.put("closeFocusedView", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                closeFocusedLogView();
            }
        });

        // Close All LogViews (Ctrl+Shift+F4)
        inputMap.put(KeyStroke.getKeyStroke("control shift F4"), "closeAllViews");
        actionMap.put("closeAllViews", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                closeAllLogViews();
            }
        });

        // Search (Ctrl+F)
        inputMap.put(KeyStroke.getKeyStroke("control F"), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openSearchDialog();
            }
        });
    }

    private void openQuickGuide() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                java.net.URL resourceUrl = getClass().getResource("/quick_guide.html");
                if (resourceUrl != null) {
                    if ("file".equals(resourceUrl.getProtocol())) {
                        Desktop.getDesktop().browse(resourceUrl.toURI());
                    } else {
                        // It's likely in a JAR, extract to temp file
                        InputStream in = getClass().getResourceAsStream("/quick_guide.html");
                        File tempFile = File.createTempFile("quick_guide", ".html");
                        tempFile.deleteOnExit();
                        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Desktop.getDesktop().browse(tempFile.toURI());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Quick guide not found!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not open quick guide: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private SearchDialog searchDialog;

    private void openSearchDialog() {
        if (logViews.isEmpty())
            return;

        LogView current = focusedLogView;
        if (current == null) {
            current = logViews.get(0);
        }

        if (searchDialog == null || !searchDialog.isVisible()) {
            searchDialog = new SearchDialog(this, logViews, current);
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

    private void closeFocusedLogView() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null)
            return;

        // Traverse up to find the LogView
        Component parent = focusOwner;
        while (parent != null && !(parent instanceof LogView)) {
            parent = parent.getParent();
        }

        if (parent instanceof LogView) {
            onClose((LogView) parent);
        } else if (focusedLogView != null) {
            onClose(focusedLogView);
        }
    }

    private void closeAllLogViews() {
        if (logViews.isEmpty())
            return;

        logViews.clear();
        minimizedViews.clear();
        focusedLogView = null;
        if (searchDialog != null && searchDialog.isVisible()) {
            searchDialog.dispose();
        }
        rebuildLayout();
        updateLogFileMenu();
    }

    private void changeFontSize(int delta) {
        fontSize = Math.max(6, Math.min(48, fontSize + delta));
        updateFontSizeForAllViews();
    }

    private void updateFontSizeForAllViews() {
        for (LogView view : logViews) {
            view.updateFontSize(fontSize);
        }
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
        statusBar.setStatus("Loading files", true);
        new Thread(() -> {
            try {
                List<String> paths = new ArrayList<>();
                for (File f : files) {
                    paths.add(f.getAbsolutePath());
                }
                loadAndMergeByPath(paths.toArray(new String[0]));
            } finally {
                statusBar.setStatus("Ready", false);
            }
        }).start();
    }

    private void handleArguments(String[] args) {
        List<File> filesToOpen = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--open=")) {
                String pathsStr = arg.substring("--open=".length());
                String[] paths = pathsStr.split(",");
                for (String p : paths) {
                    filesToOpen.add(new File(p));
                }
            }
        }
        if (!filesToOpen.isEmpty()) {
            backgroundLoadFiles(filesToOpen);
        }
    }

    private void loadAndMergeByPath(String[] paths) {
        Map<String, LogGroup> appGroups = new HashMap<>();

        for (String path : paths) {
            File f = new File(path);
            if (f.isDirectory()) {
                for (File child : logManager.scanVisibleFiles(f)) {
                    processFileIntoGroups(child, appGroups);
                }
            } else if (f.isFile()) {
                processFileIntoGroups(f, appGroups);
            }
        }

        // Add views for each group
        appGroups.forEach((appName, group) -> {
            Collections.sort(group.entries);
            String title = appName;
            if (group.getFileCount() > 1) {
                title += " (Auto-Merged)";
            }
            final String finalTitle = title;
            SwingUtilities.invokeLater(() -> addLogView(group.entries, finalTitle));
        });
    }

    private void processFileIntoGroups(File file, Map<String, LogGroup> groups) {
        try {
            List<LogEntry> entries = logManager.loadLog(file);

            // Group entries by their individual Source File (important for archives)
            Map<String, List<LogEntry>> entriesBySource = new HashMap<>();

            for (LogEntry entry : entries) {
                // Determine the base application name for *each* entry individually
                String sourceName = new File(entry.sourceFile()).getName(); // simple filename
                String appName = detectApplicationName(sourceName);

                // We use the appName as the key for the main groups map
                LogGroup group = groups.computeIfAbsent(appName, k -> new LogGroup());
                group.entries.add(entry);

                // Track how many distinct files contributed to this group?
                // LogGroup.fileCount is tricky if we stream entries.
                // Better: keep track of sourceFiles seen per group?
                group.sourceFiles.add(sourceName);
            }
        } catch (Exception e) {
            System.err.println("Error loading " + file.getName() + ": " + e.getMessage());
        }
    }

    private static class LogGroup {
        final List<LogEntry> entries = new ArrayList<>();
        // Use a Set to track unique files
        final java.util.Set<String> sourceFiles = new java.util.HashSet<>();

        // Helper access for existing logic
        int getFileCount() {
            return sourceFiles.size();
        }
    }

    /**
     * Group logs by "App Name" (ignores rotation numbers and common suffixes).
     */
    private String detectApplicationName(String fileName) {
        String name = fileName;
        String prev;

        do {
            prev = name;

            // Remove common end-of-string extensions
            name = name.replaceAll("(?i)\\.(gz|zip|7z|log|txt|bak|old|tmp)$", "");

            // Remove integer suffixes (rotation)
            name = name.replaceAll("\\.\\d+$", "");

            // Remove trailing dates (YYYY-MM-DD, YYYYMMDD)
            name = name.replaceAll("[-_.]?\\d{4}[-_.]?\\d{2}[-_.]?\\d{2}$", "");

            // Remove version-like suffixes
            name = name.replaceAll("[-_.]?(v|V)?\\d+(\\.\\d+)*$", "");

        } while (!name.equals(prev));

        // Final cleanup of trailing separators
        name = name.replaceAll("[-_.]+$", "");

        if (name.isEmpty())
            return fileName;

        return name;
    }

    private Matcher mRegex(Pattern p, String s) {
        return p.matcher(s);
    }

    private void openLogs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            backgroundLoadFiles(List.of(chooser.getSelectedFiles()));
        }
    }

    private void addLogView(List<LogEntry> entries, String title) {
        LogView logView = new LogView(entries, title, this::syncOtherViews, this);

        // Apply current column visibility
        columnVisibility.forEach((index, visible) -> {
            if (!visible) {
                logView.setColumnVisibility(index, false);
            }
        });

        logViews.add(logView);
        logView.updateFontSize(fontSize); // Apply current font size

        if (logViews.size() == 1) {
            updateFocusedView(logView);
        }

        rebuildLayout();
        updateLogFileMenu();
    }

    private void updateLogFileMenu() {
        boolean hasLogs = !logViews.isEmpty();
        logFileMenu.setEnabled(hasLogs);
        mergeItem.setEnabled(hasLogs);
        closeSelectedItem.setEnabled(hasLogs);

        logFileMenu.removeAll();
        for (LogView view : logViews) {
            boolean isVisible = !minimizedViews.contains(view);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(view.getTitle(), isVisible);
            item.addActionListener(e -> {
                if (item.isSelected()) {
                    minimizedViews.remove(view);
                } else {
                    if (!minimizedViews.contains(view))
                        minimizedViews.add(view);
                }
                rebuildLayout();
            });
            logFileMenu.add(item);
        }
    }

    private void rebuildLayout() {
        centerPanel.removeAll();
        currentRootComponent = null;

        // Peak maximization
        LogView maximized = logViews.stream().filter(LogView::isMaximized).findFirst().orElse(null);
        if (maximized != null) {
            centerPanel.add(maximized, BorderLayout.CENTER);
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

    @Override
    public void onClose(LogView view) {
        logViews.remove(view);
        minimizedViews.remove(view);
        if (focusedLogView == view) {
            focusedLogView = logViews.isEmpty() ? null : logViews.get(0);
            if (focusedLogView != null) {
                focusedLogView.setFocused(true);
            }
        }
        rebuildLayout();
        updateLogFileMenu();
    }

    private void closeSelectedViews() {
        List<LogView> toRemove = logViews.stream().filter(LogView::isSelected).toList();
        if (toRemove.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No log views selected.");
            return;
        }
        logViews.removeAll(toRemove);
        minimizedViews.removeAll(toRemove);
        rebuildLayout();
        updateLogFileMenu();
    }

    @Override
    public void onMinimize(LogView view) {
        if (!minimizedViews.contains(view)) {
            minimizedViews.add(view);
        }
        rebuildLayout();
        updateLogFileMenu();
    }

    @Override
    public void onMaximize(LogView view) {
        // If one is maximized, all others are hidden in currentRootComponent logic
        // But we should ensure only one can be maximized? Or just rebuildLayout handles
        // it.
        rebuildLayout();
        updateLogFileMenu();
    }

    @Override
    public void onFocusGained(LogView view) {
        updateFocusedView(view);
    }

    private void updateFocusedView(LogView view) {
        if (focusedLogView != null && focusedLogView != view) {
            focusedLogView.setFocused(false);
        }
        focusedLogView = view;
        if (focusedLogView != null) {
            focusedLogView.setFocused(true);
        }

        if (searchDialog != null && searchDialog.isVisible()) {
            searchDialog.setCurrentView(focusedLogView);
        }
    }

    private void syncOtherViews(LogView sourceView, LocalDateTime timestamp) {
        for (LogView view : logViews) {
            if (view != sourceView) {
                view.scrollToTimestamp(timestamp);
            }
        }
    }

    private void mergeLogs() {
        List<LogView> selectedViews = logViews.stream().filter(LogView::isSelected).toList();
        if (selectedViews.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No log views selected.");
            return;
        }

        for (LogView view : selectedViews) {
            if (!view.hasTimestamps()) {
                JOptionPane.showMessageDialog(this,
                        "Merging is not possible because one of the selected views (" + view.getTitle()
                                + ") contains entries without timestamps.",
                        "Merge Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        List<LogEntry> allEntries = new ArrayList<>();
        for (LogView view : selectedViews) {
            allEntries.addAll(view.getEntries());
            if (!minimizedViews.contains(view)) {
                minimizedViews.add(view);
            }
        }
        Collections.sort(allEntries);
        addLogView(allEntries, "Merged View");
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
