package de.in.lsp.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import de.in.lsp.service.UpdateService;
import de.in.updraft.GithubUpdater;
import de.in.updraft.UpdateInfo;
import net.miginfocom.swing.MigLayout;

/**
 * Custom update dialog for LogSyncPro.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LspUpdateDialog extends JDialog {

	public LspUpdateDialog(Frame owner, UpdateInfo info, GithubUpdater updater, UpdateService service) {
		super(owner, "Update Available", true);

		setLayout(new MigLayout("wrap 1, fill, ins 20", "[fill, grow]", "[][fill, grow][][]"));

		JLabel titleLabel = new JLabel("A new version of LogSyncPro is available: " + info.version());
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
		add(titleLabel);

		JTextArea changelogArea = new JTextArea(info.changelog());
		changelogArea.setEditable(false);
		changelogArea.setLineWrap(true);
		changelogArea.setWrapStyleWord(true);
		add(new JScrollPane(changelogArea), "height 200:200:400");

		JCheckBox skipCheckbox = new JCheckBox("Don't notify me about this version again");
		add(skipCheckbox);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		JButton updateButton = new JButton("Update Now");
		JButton closeButton = new JButton("Close");

		updateButton.addActionListener(e -> {
			new Thread(() -> {
				try {
					updater.performUpdate(info);
				} catch (Exception ex) {
					de.in.lsp.util.LspLogger.error("Update execution failed", ex);
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE));
				}
			}).start();
			dispose();
		});

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
}
