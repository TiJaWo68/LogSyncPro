package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * Tests for ensuring the Source column is only visible when multiple unique
 * sources exist.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewSourceColumnTest {

    @Test
    public void testSourceColumnHiddenForSingleSource() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Setup Single Source Data
            List<LogEntry> entries = new ArrayList<>();
            entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg1", "file1.log",
                    null));
            entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg2", "file1.log",
                    null));

            LogView logView = createLogView(entries);

            // Check Source Column (Index 6)
            TableColumn col = getColumnByModelIndex(logView.getTable().getColumnModel(), 6);
            assertNull(col, "Source column should be hidden for single source");
        });
    }

    @Test
    public void testSourceColumnVisibleForMultipleSources() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Setup Multi Source Data
            List<LogEntry> entries = new ArrayList<>();
            entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg1", "file1.log",
                    null));
            entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg2", "file2.log",
                    null));

            LogView logView = createLogView(entries);

            // Check Source Column (Index 6)
            TableColumn col = getColumnByModelIndex(logView.getTable().getColumnModel(), 6);
            assertNotNull(col, "Source column should be visible for multiple sources");
        });
    }

    @Test
    public void testSourceColumnHiddenForNoSource() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Setup No Source Data
            List<LogEntry> entries = new ArrayList<>();
            entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg1", null, null));

            LogView logView = createLogView(entries);

            // Check Source Column (Index 6)
            TableColumn col = getColumnByModelIndex(logView.getTable().getColumnModel(), 6);
            assertNull(col, "Source column should be hidden for no source");
        });
    }

    private LogView createLogView(List<LogEntry> entries) {
        return new LogView(entries, "Test View", null, new LogViewListener() {
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
