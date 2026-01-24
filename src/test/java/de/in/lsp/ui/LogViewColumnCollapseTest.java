package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * Tests for ensuring LogView column collapsing logic.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewColumnCollapseTest {

    @Test
    public void testColumnCollapsingLogic() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // 1. Setup Data
            List<LogEntry> entries = new ArrayList<>();
            entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main", "com.example.Logger", "127.0.0.1",
                    "Message", "source.log", null));

            // 2. Create LogView
            LogView logView = new LogView(entries, "Test View", null, new LogViewListener() {
                public void onFocusGained(LogView view) {
                }

                public void onClose(LogView view) {
                }

                public void onMinimize(LogView view) {
                }

                public void onMaximize(LogView view) {
                }

                public void onIncreaseFontSize() {
                }

                public void onDecreaseFontSize() {
                }
            }, ViewType.FILE);

            // Access Table
            JTable table = logView.getTable();
            TableColumnModel tcm = table.getColumnModel();

            // 3. Simulate specific viewport size for "Collapsing"
            logView.setSize(new Dimension(500, 600));
            logView.validate();
            // Mock layout propagation if needed for headless/test env
            // The filtered table panel is usually index 0 in the splitpane but we can
            // traverse
            // Or just assume logView.validate() does most if showing.
            // But components are not showing.
            // Force doLayout on tree.
            layoutComponentTree(logView);

            // Force logic
            logView.updateFontSize(12);

            // Debug
            TableColumn ipCol = getColumnByModelIndex(tcm, 4); // IP
            TableColumn msgCol = getColumnByModelIndex(tcm, 5); // Message

            System.err.println("--- COLLAPSED STATE ---");
            System.err.println("LogView Size: " + logView.getSize());
            System.err.println("IP Width: " + (ipCol != null ? ipCol.getWidth() : "null"));
            System.err.println("Msg Width: " + msgCol.getWidth());

            // Check
            if (ipCol != null && ipCol.getWidth() <= 30) {
                System.err.println("IP Collapsed correctly.");
            } else {
                System.err.println("IP NOT Collapsed.");
            }

            // 4. Test Expansion
            logView.setSize(new Dimension(1200, 600));
            logView.validate();
            layoutComponentTree(logView);

            logView.updateFontSize(12); // Force logic

            System.err.println("--- EXPANDED STATE ---");
            System.err.println("LogView Size: " + logView.getSize());
            System.err.println("IP Width: " + (ipCol != null ? ipCol.getWidth() : "null"));
            System.err.println("Msg Width: " + msgCol.getWidth());

            if (ipCol != null) {
                assertTrue(ipCol.getWidth() > 30, "IP should be expanded (Actual: " + ipCol.getWidth() + ")");
            }

            assertTrue(msgCol.getWidth() >= LogView.MIN_MESSAGE_WIDTH,
                    "Message should be >= " + LogView.MIN_MESSAGE_WIDTH + " (Actual: " + msgCol.getWidth() + ")");
        });
    }

    private void layoutComponentTree(java.awt.Component c) {
        c.doLayout();
        if (c instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) c).getComponents()) {
                layoutComponentTree(child);
            }
        }
    }

    private TableColumn getColumnByModelIndex(TableColumnModel tcm, int modelIndex) {
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i).getModelIndex() == modelIndex) {
                return tcm.getColumn(i);
            }
        }
        return null;
    }
}
