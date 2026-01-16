package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.formdev.flatlaf.util.UIScale;

/**
 * A graphically appealing status bar showing memory usage.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class MemoryStatusBar extends JPanel {

    private final JProgressBar memoryBar;
    private final JLabel statusLabel;
    private final Timer refreshTimer;
    private final Timer loadingTimer;
    private int loadingStep = 0;

    public MemoryStatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setPreferredSize(new Dimension(UIScale.scale(350), UIScale.scale(18)));

        statusLabel = new JLabel(" Ready");
        statusLabel.setIconTextGap(5);

        JPanel container = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        container.setOpaque(false);
        container.add(memoryBar);

        add(statusLabel, BorderLayout.WEST);
        add(container, BorderLayout.EAST);

        refreshTimer = new Timer(2000, e -> updateMemoryInfo());
        refreshTimer.start();

        loadingTimer = new Timer(300, e -> animateLoading());

        MouseAdapter gcListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    System.gc();
                    updateMemoryInfo();
                }
            }
        };

        addMouseListener(gcListener);
        memoryBar.addMouseListener(gcListener);
        statusLabel.addMouseListener(gcListener); // Add to label too for good measure

        updateMemoryInfo();
    }

    private String statusBaseText = "Ready";

    public void setStatus(String text, boolean loading) {
        SwingUtilities.invokeLater(() -> {
            this.statusBaseText = text;
            statusLabel.setText(" " + text);
            if (loading) {
                if (!loadingTimer.isRunning()) {
                    loadingTimer.start();
                }
            } else {
                loadingTimer.stop();
                statusLabel.setText(" " + text);
            }
        });
    }

    private void animateLoading() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < (loadingStep % 4); i++) {
            dots.append(".");
        }
        statusLabel.setText(" " + statusBaseText + dots.toString());
        loadingStep++;
    }

    private void updateMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        int percent = (int) ((usedMemory * 100) / maxMemory);
        memoryBar.setValue(percent);

        // Color coding based on usage
        if (percent > 90) {
            memoryBar.setForeground(new Color(200, 50, 50)); // Red
        } else if (percent > 70) {
            memoryBar.setForeground(new Color(200, 150, 50)); // Orange
        } else {
            memoryBar.setForeground(new Color(50, 150, 50)); // Green
        }

        String info = String.format("%d%% (Used: %d MB / Max: %d MB)",
                percent,
                usedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024));

        memoryBar.setString(info);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Subtle top border for separation
        g.setColor(getBackground().darker());
        g.drawLine(0, 0, getWidth(), 0);
    }
}
