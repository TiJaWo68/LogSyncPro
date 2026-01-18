package de.in.lsp.service;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

import javax.swing.*;

import de.in.lsp.LogSyncPro;
import de.in.lsp.ui.LspUpdateDialog;
import de.in.updraft.GithubUpdater;
import de.in.updraft.UpdateChannel;
import de.in.updraft.UpdateInfo;
import de.in.updraft.source.GithubReleaseSource;

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

        String testVersion = System.getProperty("lsp.test.version");
        if (testVersion != null && !testVersion.isEmpty()) {
            this.currentVersion = testVersion;
            de.in.lsp.util.LspLogger.warn("Running with overridden test version: " + testVersion);
        } else {
            this.currentVersion = currentVersion;
        }

        // Repo TiJaWo68/LogSyncPro
        UpdateChannel configuredChannel = channel;
        String channelProp = System.getProperty("lsp.update.channel");
        if (channelProp != null && !channelProp.isEmpty()) {
            try {
                configuredChannel = UpdateChannel.valueOf(channelProp.toUpperCase());
                de.in.lsp.util.LspLogger.info("Update channel overridden to: " + configuredChannel);
            } catch (IllegalArgumentException e) {
                de.in.lsp.util.LspLogger.warn("Invalid update channel property: " + channelProp);
            }
        }

        Path appJar;
        try {
            appJar = Paths.get(LogSyncPro.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            de.in.lsp.util.LspLogger.error("Failed to determine application JAR path", e);
            throw new RuntimeException(e);
        }

        this.updater = new GithubUpdater(this.currentVersion,
                new GithubReleaseSource("TiJaWo68", "LogSyncPro", configuredChannel), appJar);
    }

    /**
     * Checks for updates asynchronously.
     */
    public void checkForUpdatesAsync(boolean manual) {
        CompletableFuture.runAsync(() -> {
            try {
                de.in.lsp.util.LspLogger.info("Checking for updates... (Manual: " + manual + ")");
                UpdateInfo info = updater.checkForUpdates();
                if (info != null) {
                    de.in.lsp.util.LspLogger.info("Update found: " + info.version());
                    if (manual || !shouldSkip(info.version())) {
                        SwingUtilities.invokeLater(() -> showDialog(info));
                    }
                } else {
                    de.in.lsp.util.LspLogger.info("No update found.");
                    if (manual) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner,
                                "Your version " + currentVersion + " is up to date.",
                                "No Update Available", JOptionPane.INFORMATION_MESSAGE));
                    }
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
