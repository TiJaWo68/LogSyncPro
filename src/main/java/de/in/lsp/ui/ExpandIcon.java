package de.in.lsp.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * An icon that displays opposing chevrons (< >) to symbolize expansion.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ExpandIcon implements Icon {

	private final Color color;
	private final int size;

	public ExpandIcon(Color color) {
		this(color, 12);
	}

	public ExpandIcon(Color color, int size) {
		this.color = color;
		this.size = size;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(color);
		g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		int h = size;
		int w = size;

		// Chevron Left (<) x: 0..w/2
		int cx = x + (w / 4);
		int cy = y + (h / 2);

		// Draw Left Chevron
		g2.drawLine(cx + 2, cy - 3, cx - 1, cy);
		g2.drawLine(cx - 1, cy, cx + 2, cy + 3);

		// Chevron Right (>)
		int cx2 = x + (w * 3 / 4);

		// Draw Right Chevron
		g2.drawLine(cx2 - 2, cy - 3, cx2 + 1, cy);
		g2.drawLine(cx2 + 1, cy, cx2 - 2, cy + 3);

		g2.dispose();
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}
}
