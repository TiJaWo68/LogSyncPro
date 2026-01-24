package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import de.in.lsp.model.LogEntry;

/**
 * A side/bottom panel that displays the full details of a single selected
 * LogEntry.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogDetailView extends JPanel {

    private int currentFontSize = 12;
    private final JTextArea messageArea;

    // We only need one formatter
    private static final DateTimeFormatter DNF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final JPanel metaPanel;
    private final Runnable onClose;
    private final Runnable onDetach;

    public LogDetailView(Runnable onDetach, Runnable onClose) {
        this.onDetach = onDetach;
        this.onClose = onClose;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Header Panel (BorderLayout to separate Meta and Buttons)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        // Metadata Panel - Use WrapLayout to handle wrapping of fields
        metaPanel = new JPanel(new de.in.lsp.ui.helper.WrapLayout(FlowLayout.LEFT, 10, 5));
        metaPanel.setOpaque(false);

        // CRITICAL: Force re-layout when width changes to allow WrapLayout to
        // recalculate height based on new width
        metaPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    metaPanel.revalidate();
                    // Also revalidate the parent (headerPanel) so it adjusts its height preference
                    if (metaPanel.getParent() != null) {
                        metaPanel.getParent().revalidate();
                    }
                });
            }
        });

        headerPanel.add(metaPanel, BorderLayout.CENTER);

        // Buttons Panel (Top-Right)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        JButton detachBtn = new JButton("↗"); // North East Arrow
        detachBtn.setMargin(new Insets(0, 6, 0, 6));
        detachBtn.setToolTipText("Detach Detail View");
        detachBtn.addActionListener(e -> {
            if (this.onDetach != null)
                this.onDetach.run();
        });

        JButton closeBtn = new JButton("×");
        closeBtn.setMargin(new Insets(0, 6, 0, 6));
        closeBtn.setToolTipText("Close Detail View");
        closeBtn.addActionListener(e -> {
            if (this.onClose != null)
                this.onClose.run();
        });

        buttonPanel.add(detachBtn);
        buttonPanel.add(Box.createHorizontalStrut(2));
        buttonPanel.add(closeBtn);

        // Align buttons to top
        JPanel buttonContainer = new JPanel(new BorderLayout());
        buttonContainer.setOpaque(false);
        buttonContainer.add(buttonPanel, BorderLayout.NORTH);

        headerPanel.add(buttonContainer, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Message Area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true); // Wrap at word boundaries

        add(new JScrollPane(messageArea), BorderLayout.CENTER);
    }

    public void setEntry(LogEntry entry) {
        metaPanel.removeAll();

        if (entry != null) {
            addField("Time", entry.timestamp() != null ? DNF.format(entry.timestamp()) : null);
            addField("Level", entry.level() != null ? entry.level().toString() : null);
            addField("Thread", entry.thread());
            addField("Logger", entry.loggerName());
            addField("IP", entry.ip());
            addField("Source", entry.sourceFile());

            messageArea.setText(entry.message() != null ? entry.message() : "");
            messageArea.setCaretPosition(0);
        } else {
            messageArea.setText("");
        }

        metaPanel.revalidate();
        metaPanel.repaint();
    }

    private void addField(String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        JTextField valField = new JTextField(value) {
            @Override
            public java.awt.Dimension getPreferredSize() {
                java.awt.Dimension d = super.getPreferredSize();
                java.awt.Container parent = getParent();
                if (parent != null && parent.getWidth() > 0) {
                    // 20px padding (10 left + 10 right from WrapLayout gap/insets approximation)
                    int maxW = parent.getWidth() - 20;
                    if (d.width > maxW) {
                        d.width = maxW;
                    }
                }
                return d;
            }
        };
        valField.setEditable(false);
        valField.setOpaque(false);

        // Apply current font size
        Font oldFont = valField.getFont();
        Font newFont = oldFont.deriveFont((float) currentFontSize);
        valField.setFont(newFont);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                label);

        // Make the title bold
        java.awt.Font font = titledBorder.getTitleFont();
        if (font == null) {
            // Fallback if generic font is null (common in some LAFs)
            font = valField.getFont();
        }
        if (font != null) {
            titledBorder.setTitleFont(font.deriveFont(Font.BOLD).deriveFont((float) currentFontSize));
        }

        valField.setBorder(BorderFactory.createCompoundBorder(
                titledBorder,
                BorderFactory.createEmptyBorder(0, 4, 2, 4)));

        valField.setColumns(Math.min(value.length() + 1, 120));

        metaPanel.add(valField);
    }

    public void setFontSize(int size) {
        this.currentFontSize = size;
        Font oldFont = messageArea.getFont();
        Font newFont = oldFont.deriveFont((float) size);
        messageArea.setFont(newFont);

        // Update TitledBorders
        for (java.awt.Component c : metaPanel.getComponents()) {
            if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                tf.setFont(newFont); // Update text field font too

                if (tf.getBorder() instanceof javax.swing.border.CompoundBorder) {
                    javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) tf.getBorder();
                    if (cb.getOutsideBorder() instanceof TitledBorder) {
                        TitledBorder tb = (TitledBorder) cb.getOutsideBorder();
                        tb.setTitleFont(newFont.deriveFont(Font.BOLD));
                    }
                }
            }
        }

        // Force relayout if fonts changed significantly
        metaPanel.revalidate();
        metaPanel.repaint();
    }
}
