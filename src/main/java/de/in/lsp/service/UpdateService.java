package de.in.lsp.service;

import de.in.updraft.GithubUpdater;
import de.in.updraft.UpdateChannel;
import de.in.updraft.UpdateInfo;
import de.in.updraft.source.GithubReleaseSource;
import de.in.lsp.ui.LspUpdateDialog;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

/**
 * Service to manage update checks and preferences.
 */
public class UpdateService {
    private static final String PREF_NODE = "de.in.lsp.update";
    private static final String PREF_SKIP_VERSION = "skip_version";

    private final GithubUpdater updater;
    private final String currentVersion;

    private final JFrame owner;

    public UpdateService(JFrame owner, String currentVersion, UpdateChannel channel) {
        this.owner = owner;
        this.currentVersion = currentVersion;
        // Repo TiJaWo68/LogSyncPro
        this.updater = new GithubUpdater(currentVersion,
                new GithubReleaseSource("TiJaWo68", "LogSyncPro", channel));
    }

    /**
     * Checks for updates asynchronously.
     */
    public void checkForUpdatesAsync(boolean manual) {
        CompletableFuture.runAsync(() -> {
            try {
                UpdateInfo info = updater.checkForUpdates();
                if (info != null) {
                    if (manual || !shouldSkip(info.version())) {
                        SwingUtilities.invokeLater(() -> showDialog(info));
                    }
                } else if (manual) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner,
                            "Your version " + currentVersion + " is up to date.",
                            "No Update Available", JOptionPane.INFORMATION_MESSAGE));
                }
            } catch (Exception e) {
                de.in.lsp.util.LspLogger.error("Failed to check for updates", e);
                if (manual) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner,
                            "Failed to check for updates: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
                }
            }
        });
    }

    private boolean shouldSkip(String version) {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        return version.equals(prefs.get(PREF_SKIP_VERSION, ""));
    }

    public void skipVersion(String version) {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        prefs.put(PREF_SKIP_VERSION, version);
    }

    private void showDialog(UpdateInfo info) {
        LspUpdateDialog dialog = new LspUpdateDialog(owner, info, updater, this);
        dialog.setVisible(true);
    }
}
