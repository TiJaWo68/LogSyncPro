package de.in.lsp.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * A standard filter icon for log view components.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class FilterIcon implements Icon {
    private final Color color;

    public FilterIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        int[] px = { x + 2, x + 10, x + 10, x + 6, x + 6, x + 2 };
        int[] py = { y + 2, y + 2, y + 6, y + 10, y + 6, y + 6 };
        g2.fillPolygon(px, py, 6);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return 12;
    }

    @Override
    public int getIconHeight() {
        return 12;
    }
}
