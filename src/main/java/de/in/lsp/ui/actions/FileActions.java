package de.in.lsp.ui.actions;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import de.in.lsp.service.LogFileService;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.ViewManager;
import de.in.lsp.util.LspLogger;

/**
 * Handles file-related actions like loading logs from disk.
 * Connects the UI to the LogFileService for background processing.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class FileActions {
    private final ViewManager viewManager;
    private final LogFileService logFileService = new LogFileService();

    public FileActions(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    public void backgroundLoadFiles(List<File> files, Consumer<String> statusConsumer,
            LogView.LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
        LspLogger.info("Starting background loading of " + files.size() + " files.");
        logFileService.backgroundLoadFiles(files, statusConsumer, (appName, group) -> {
            String title = appName;
            if (group.getFileCount() > 1) {
                title += " (Auto-Merged)";
            }
            final String finalTitle = title;
            LspLogger.info("Loaded application '" + appName + "' with " + group.getEntries().size() + " entries.");
            SwingUtilities.invokeLater(
                    () -> viewManager.addLogView(group.getEntries(), finalTitle, columnVisibility, listener,
                            LogView.ViewType.FILE));
        });
    }
}
