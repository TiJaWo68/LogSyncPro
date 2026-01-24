package de.in.lsp;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import de.in.lsp.service.UpdateService;
import de.in.lsp.ui.LogFileTransferHandler;
import de.in.lsp.ui.LogSyncProMenu;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.LogViewListener;
import de.in.lsp.ui.MemoryStatusBar;
import de.in.lsp.ui.ViewManager;
import de.in.lsp.ui.ViewType;
import de.in.lsp.ui.actions.FileActions;
import de.in.lsp.ui.actions.HelpActions;
import de.in.lsp.ui.actions.RemoteActions;
import de.in.lsp.ui.actions.ViewActions;
import de.in.lsp.util.LspLogger;
import de.in.lsp.util.VersionUtil;

/**
 * Main application class for LogSyncPro.
 * Manages the main window shell and delegates logic to specialized components.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogSyncPro extends JFrame implements LogViewListener {

    private ViewManager viewManager;
    private ViewActions viewActions;
    private RemoteActions remoteActions;
    private FileActions fileActions;
    private HelpActions helpActions;
    private de.in.lsp.service.ReceiverManager receiverManager;
    private UpdateService updateService;

    private JDesktopPane centerPanel;
    private MemoryStatusBar statusBar;
    private LogSyncProMenu appMenu;
    private LogView internalLogView;
    private final Map<Integer, Boolean> columnVisibility = new HashMap<>();

    private static final String[] COLUMN_NAMES = { "Timestamp", "Level", "Thread", "Logger", "IP", "Message",
            "Source" };

    private static final String PREF_X = "frame.x";
    private static final String PREF_Y = "frame.y";
    private static final String PREF_WIDTH = "frame.width";
    private static final String PREF_HEIGHT = "frame.height";
    private static final String PREF_STATE = "frame.state";
    private static final String PREF_FONT_SIZE = "logview.fontsize";

    public LogSyncPro(String[] args) {
        String version = VersionUtil.retrieveVersionFromPom("de.in.lsp", "LogSyncPro");
        setTitle("LogSyncPro " + version);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveFrameState();
                LspLogger.info("Exiting application.");
                System.exit(0);
            }
        });
        setMinimumSize(new Dimension(1000, 700));

        loadFrameState();
        setupIcon();
        initColumnVisibility();
        setupUI();
        this.receiverManager = new de.in.lsp.service.ReceiverManager(viewManager, this, columnVisibility);
        this.updateService = new UpdateService(this, version, de.in.updraft.UpdateChannel.STABLE);
        setupMenuBar();
        setupInternalLogging();

        this.updateService.checkForUpdatesAsync(false);

        handleArguments(args);
        appMenu.updateReceiverMenu();
        LspLogger.info("LogSyncPro started.");
    }

    private void setupInternalLogging() {
        internalLogView = viewManager.addLogView(new ArrayList<>(), "Internal Log", columnVisibility, this,
                ViewType.INTERNAL);
        internalLogView.hideColumnPermanently(4); // Hide IP column
        internalLogView.hideColumnPermanently(6); // Hide Source column
        viewManager.minimizeView(internalLogView);
        LspLogger.addListener(entry -> {
            SwingUtilities.invokeLater(() -> {
                viewManager.addEntry(internalLogView, entry);
            });
        });
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
        Runnable openLogsAction = this::openLogs;
        Runnable exitAction = () -> {
            saveFrameState();
            LspLogger.info("Exiting application.");
            System.exit(0);
        };

        this.appMenu = new LogSyncProMenu(this, viewActions, remoteActions, helpActions, receiverManager, viewManager,
                columnVisibility, updateService, openLogsAction, exitAction);
        setJMenuBar(appMenu);
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        centerPanel = new JDesktopPane() {
            private FlatSVGIcon watermark;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (viewManager != null && !viewManager.getLogViews().isEmpty())
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
        viewManager.setFontSize(loadFontSize());
        viewActions = new ViewActions(this, viewManager);
        remoteActions = new RemoteActions(this, viewManager);
        fileActions = new FileActions(viewManager);
        helpActions = new HelpActions(this);

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
        inputMap.put(KeyStroke.getKeyStroke("control PLUS"), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int newSize = Math.max(8, Math.min(40, viewManager.getFontSize() + 1));
                viewManager.setFontSize(newSize);
                saveFontSize(newSize);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control MINUS"), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int newSize = Math.max(8, Math.min(40, viewManager.getFontSize() - 1));
                viewManager.setFontSize(newSize);
                saveFontSize(newSize);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control W"), "closeFocusedView");
        actionMap.put("closeFocusedView", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                viewActions.closeFocusedLogView();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control shift F4"), "closeAllViews");
        actionMap.put("closeAllViews", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                viewActions.closeAllLogViews();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control F"), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                viewActions.openSearchDialog();
            }
        });
    }

    private void setupDragAndDrop() {
        setTransferHandler(new LogFileTransferHandler(this::backgroundLoadFiles));
    }

    private void backgroundLoadFiles(List<File> files) {
        fileActions.backgroundLoadFiles(files,
                status -> statusBar.setStatus(status, status.contains("Loading")),
                this, columnVisibility);
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
            } else if (arg.startsWith("--receiver=")) {
                String portsStr = arg.substring("--receiver=".length());
                String[] ports = portsStr.split(",");
                for (String p : ports) {
                    try {
                        receiverManager.startReceiver(Integer.parseInt(p.trim()));
                    } catch (Exception e) {
                        LspLogger.error("Failed to auto-start receiver on port " + p, e);
                    }
                }
            }
        }

        if (sshValue != null) {
            remoteActions.importFromK8sAutomated(sshValue, fetchValue, this, columnVisibility);
        }

        if (!filesToOpen.isEmpty()) {
            backgroundLoadFiles(filesToOpen);
        }
    }

    private void openLogs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = chooser.getSelectedFiles();
            LspLogger.info("Opening " + selectedFiles.length + " files via file chooser.");
            backgroundLoadFiles(List.of(selectedFiles));
        }
    }

    private void updateLogFileMenu() {
        if (appMenu != null) {
            appMenu.updateLogFileMenu();
        }
    }

    @Override
    public void onClose(LogView view) {
        if (view == internalLogView) {
            viewManager.minimizeView(view);
            return;
        }
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

    @Override
    public void onIncreaseFontSize() {
        int newSize = Math.max(8, Math.min(40, viewManager.getFontSize() + 1));
        viewManager.setFontSize(newSize);
        saveFontSize(newSize);
    }

    @Override
    public void onDecreaseFontSize() {
        int newSize = Math.max(8, Math.min(40, viewManager.getFontSize() - 1));
        viewManager.setFontSize(newSize);
        saveFontSize(newSize);
    }

    private void loadFrameState() {
        Preferences prefs = Preferences.userNodeForPackage(LogSyncPro.class);
        int x = prefs.getInt(PREF_X, -1);
        int y = prefs.getInt(PREF_Y, -1);
        int width = prefs.getInt(PREF_WIDTH, 1000);
        int height = prefs.getInt(PREF_HEIGHT, 700);
        int state = prefs.getInt(PREF_STATE, NORMAL);

        if (x != -1 && y != -1) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }
        setSize(width, height);
        setExtendedState(state);
    }

    private void saveFrameState() {
        Preferences prefs = Preferences.userNodeForPackage(LogSyncPro.class);
        int state = getExtendedState();

        prefs.putInt(PREF_X, getX());
        prefs.putInt(PREF_Y, getY());
        prefs.putInt(PREF_WIDTH, getWidth());
        prefs.putInt(PREF_HEIGHT, getHeight());
        prefs.putInt(PREF_STATE, state);
    }

    private int loadFontSize() {
        Preferences prefs = Preferences.userNodeForPackage(LogSyncPro.class);
        return prefs.getInt(PREF_FONT_SIZE, 12);
    }

    private void saveFontSize(int size) {
        Preferences prefs = Preferences.userNodeForPackage(LogSyncPro.class);
        prefs.putInt(PREF_FONT_SIZE, size);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            LogSyncPro app = new LogSyncPro(args);
            app.setVisible(true);
        });
    }
}
