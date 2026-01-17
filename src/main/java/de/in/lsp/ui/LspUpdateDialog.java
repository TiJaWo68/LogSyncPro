package de.in.lsp.ui;

import de.in.updraft.GithubUpdater;
import de.in.updraft.UpdateInfo;
import de.in.lsp.service.UpdateService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Custom update dialog for LogSyncPro.
 */
public class LspUpdateDialog extends JDialog {
    private final UpdateInfo info;
    private final GithubUpdater updater;
    private final UpdateService service;
    private final JCheckBox skipCheckbox;

    public LspUpdateDialog(Frame owner, UpdateInfo info, GithubUpdater updater, UpdateService service) {
        super(owner, "Update Available", true);
        this.info = info;
        this.updater = updater;
        this.service = service;

        setLayout(new MigLayout("wrap 1, fill, ins 20", "[fill, grow]", "[][fill, grow][][]"));

        JLabel titleLabel = new JLabel("A new version of LogSyncPro is available: " + info.version());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel);

        JTextArea changelogArea = new JTextArea(info.changelog());
        changelogArea.setEditable(false);
        changelogArea.setLineWrap(true);
        changelogArea.setWrapStyleWord(true);
        add(new JScrollPane(changelogArea), "height 200:200:400");

        skipCheckbox = new JCheckBox("Don't notify me about this version again");
        add(skipCheckbox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton updateButton = new JButton("Update Now");
        JButton closeButton = new JButton("Close");

        updateButton.addActionListener(e -> startUpdate());
        closeButton.addActionListener(e -> {
            if (skipCheckbox.isSelected()) {
                service.skipVersion(info.version());
            }
            dispose();
        });

        buttonPanel.add(updateButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, "right");

        pack();
        setLocationRelativeTo(owner);
    }

    private void startUpdate() {
        // Actually we just call updater.performUpdate
        new Thread(() -> {
            try {
                updater.performUpdate(info);
            } catch (Exception ex) {
                de.in.lsp.util.LspLogger.error("Update execution failed", ex);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
        dispose();
    }
}
