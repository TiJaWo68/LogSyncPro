package de.in.lsp.ui.actions;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Handles help-related actions like opening the quick guide.
 * Provides access to documentation and version information.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class HelpActions {
    private final JFrame parentFrame;

    public HelpActions(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    public void openQuickGuide() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                java.net.URL resourceUrl = getClass().getResource("/quick_guide.html");
                if (resourceUrl != null) {
                    if ("file".equals(resourceUrl.getProtocol())) {
                        Desktop.getDesktop().browse(resourceUrl.toURI());
                    } else {
                        InputStream in = getClass().getResourceAsStream("/quick_guide.html");
                        File tempFile = File.createTempFile("quick_guide", ".html");
                        tempFile.deleteOnExit();
                        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Desktop.getDesktop().browse(tempFile.toURI());
                    }
                } else {
                    JOptionPane.showMessageDialog(parentFrame, "Quick guide not found!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, "Could not open quick guide: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
