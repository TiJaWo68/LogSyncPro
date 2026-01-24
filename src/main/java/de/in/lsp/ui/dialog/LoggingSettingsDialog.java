package de.in.lsp.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.in.lsp.util.LspLogger;

/**
 * Dialog to configure internal logging settings.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LoggingSettingsDialog extends JDialog {

	public LoggingSettingsDialog(Frame owner) {
		super(owner, "Logging Einstellungen", true);
		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(300, 150));

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel("Interne Log-Ebene:"), gbc);

		gbc.gridx = 1;
		JComboBox<LspLogger.LogLevel> levelCombo = new JComboBox<>(LspLogger.LogLevel.values());
		levelCombo.setSelectedItem(LspLogger.getThreshold());
		content.add(levelCombo, gbc);

		add(content, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> {
			LspLogger.setThreshold((LspLogger.LogLevel) levelCombo.getSelectedItem());
			dispose();
		});
		buttonPanel.add(okButton);

		JButton cancelButton = new JButton("Abbrechen");
		cancelButton.addActionListener(e -> dispose());
		buttonPanel.add(cancelButton);

		add(buttonPanel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}
}
