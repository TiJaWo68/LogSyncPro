package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import de.in.lsp.model.LogEntry;

/**
 * A side/bottom panel that displays the full details of a single selected
 * LogEntry.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogDetailView extends JPanel {

    private final JTextArea messageArea;
    private final JTextField timestampField;
    private final JTextField levelField;
    private final JTextField threadField;
    private final JTextField loggerField;
    private final JTextField sourceField;
    private final Runnable onClose;

    private static final DateTimeFormatter DNF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public LogDetailView(Runnable onClose) {
        this.onClose = onClose;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Header Panel (BorderLayout to separate Meta and Close button)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        // Metadata Panel
        JPanel metaPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 5);
        gbc.weightx = 0.0;

        timestampField = createReadOnlyField();
        levelField = createReadOnlyField();
        threadField = createReadOnlyField();
        loggerField = createReadOnlyField();
        sourceField = createReadOnlyField();

        addLabelAndField(metaPanel, "Time:", timestampField, gbc, 0);
        addLabelAndField(metaPanel, "Level:", levelField, gbc, 1);
        addLabelAndField(metaPanel, "Thread:", threadField, gbc, 2);

        // Next Row
        addLabelAndField(metaPanel, "Logger:", loggerField, gbc, 0, 1);
        addLabelAndField(metaPanel, "Source:", sourceField, gbc, 1, 1);

        // Filler for Meta Panel
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        metaPanel.add(Box.createHorizontalGlue(), gbc);

        headerPanel.add(metaPanel, BorderLayout.CENTER);

        // Close Button Panel (Top-Right)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        JButton closeBtn = new JButton("×");
        closeBtn.setMargin(new Insets(0, 6, 0, 6));
        closeBtn.setToolTipText("Close Detail View");
        closeBtn.addActionListener(e -> {
            if (onClose != null)
                onClose.run();
        });
        buttonPanel.add(closeBtn);

        headerPanel.add(buttonPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Message Area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageArea.setLineWrap(false); // Let it scroll horizontally for long lines if preferred, or true

        add(new JScrollPane(messageArea), BorderLayout.CENTER);
    }

    private void addLabelAndField(JPanel panel, String labelText, JTextField field, GridBagConstraints gbc, int x) {
        addLabelAndField(panel, labelText, field, gbc, x, 0);
    }

    private void addLabelAndField(JPanel panel, String labelText, JTextField field, GridBagConstraints gbc, int x,
            int y) {
        gbc.gridx = x * 2;
        gbc.gridy = y;
        gbc.weightx = 0.0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = x * 2 + 1;
        gbc.weightx = 0.1;
        panel.add(field, gbc);
    }

    private JTextField createReadOnlyField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        return field;
    }

    public void setEntry(LogEntry entry) {
        if (entry == null) {
            clear();
            return;
        }
        timestampField.setText(entry.timestamp() != null ? DNF.format(entry.timestamp()) : "");
        levelField.setText(entry.level() != null ? entry.level().toString() : "");
        threadField.setText(entry.thread() != null ? entry.thread() : "");
        loggerField.setText(entry.loggerName() != null ? entry.loggerName() : "");
        sourceField.setText(entry.sourceFile() != null ? entry.sourceFile() : "");
        messageArea.setText(entry.message() != null ? entry.message() : "");
        messageArea.setCaretPosition(0);
    }

    private void clear() {
        timestampField.setText("");
        levelField.setText("");
        threadField.setText("");
        loggerField.setText("");
        sourceField.setText("");
        messageArea.setText("");
    }
}
