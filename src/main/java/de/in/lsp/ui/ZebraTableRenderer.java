package de.in.lsp.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom JTable cell renderer that implements a zebra-striped design
 * to improve row readability in large log tables.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ZebraTableRenderer extends DefaultTableCellRenderer {

    private static final Color WARN_COLOR = new Color(255, 255, 200); // Light Yellow
    private static final Color ERROR_COLOR = new Color(255, 200, 200); // Light Red

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        JLabel label = (JLabel) c;
        label.setIcon(null);
        label.setToolTipText(null);

        int modelColumn = table.convertColumnIndexToModel(column);

        if (!isSelected) {
            // Level-specific background and foreground
            if (modelColumn == 1) { // Level column
                String level = String.valueOf(value).toUpperCase();
                if (level.contains("WARN")) {
                    c.setBackground(WARN_COLOR);
                    c.setForeground(Color.BLACK);
                } else if (level.contains("ERROR") || level.contains("FATAL")) {
                    c.setBackground(ERROR_COLOR);
                    c.setForeground(Color.BLACK);
                } else {
                    applyZebraBackground(table, c, row);
                    c.setForeground(table.getForeground());
                }
            } else {
                applyZebraBackground(table, c, row);
                c.setForeground(table.getForeground());
            }
        }

        // Source column special rendering (Icon or empty)
        if (modelColumn == 6) { // Source
            String source = String.valueOf(value);
            label.setText(""); // Always clear text to prevent "letters"
            label.setToolTipText(source);

            // Use icons for source to keep it narrow (24px)
            if (table.getModel() instanceof LogTableModel ltm && ltm.getUniqueSourceCount() > 1) {
                label.setIcon(new ColorBlockIcon(getColorForSource(source)));
            } else {
                label.setIcon(null);
            }
            label.setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            label.setHorizontalAlignment(SwingConstants.LEADING);
        }

        // Hide "UNKNOWN" placeholder values
        if ("UNKNOWN".equals(String.valueOf(value))) {
            label.setText("");
        }

        return c;
    }

    private void applyZebraBackground(JTable table, Component c, int row) {
        if (row % 2 == 0) {
            c.setBackground(table.getBackground());
        } else {
            Color bg = table.getBackground();
            int r = Math.max(0, bg.getRed() - 10);
            int g = Math.max(0, bg.getGreen() - 10);
            int b = Math.max(0, bg.getBlue() - 10);
            c.setBackground(new Color(r, g, b));
        }
    }

    private Color getColorForSource(String source) {
        if (source == null)
            return Color.GRAY;
        int hash = source.hashCode();
        return new Color(
                (hash & 0xFF0000) >> 16,
                (hash & 0x00FF00) >> 8,
                (hash & 0x0000FF),
                180 // Alpha for transparency
        );
    }

    private static class ColorBlockIcon implements Icon {
        private final Color color;

        public ColorBlockIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x + 2, y + 2, getIconWidth() - 4, getIconHeight() - 4);
            g.setColor(color.darker());
            g.drawRect(x + 2, y + 2, getIconWidth() - 4, getIconHeight() - 4);
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

}
