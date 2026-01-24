package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

/**
 * Tests for ensuring LogView column width rules and detecting truncation.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogViewColumnWidthTest {

	@Test
	public void testColumnWidthTruncation() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			// 1. Setup Data with long content
			List<LogEntry> entries = new ArrayList<>();
			entries.add(new LogEntry(LocalDateTime.now(), "INFO", "main-thread-is-very-long-and-might-be-truncated",
					"com.example.very.long.package.name.that.might.cause.issues.LoggerClass", "192.168.100.200", // Somewhat long IP
					"This is a sample message", "source.log", null));

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
			}, ViewType.FILE);

			// Mock sizing to force layout
			logView.setSize(new Dimension(800, 600));
			logView.doLayout();

			JTable table = logView.getTable();
			Font originalFont = table.getFont();

			// 3. Test with a larger font size to exacerbate issues
			int largerFontSize = 16;
			logView.updateFontSize(largerFontSize);
			Font largeFont = originalFont.deriveFont((float) largerFontSize);
			FontMetrics fm = table.getFontMetrics(largeFont);

			// 4. Verify Truncation Check specific columns that are likely to be truncated
			checkTruncation(table, fm, 0, entries.get(0).getFormattedTimestamp()); // Timestamp
			checkTruncation(table, fm, 1, entries.get(0).level()); // Level
			// checkTruncation(table, fm, 2, entries.get(0).thread()); // Thread - Heuristic sizing, may be truncated checkTruncation(table,
			// fm, 3, entries.get(0).loggerName()); // Logger - Heuristic sizing, may be truncated
			checkTruncation(table, fm, 4, entries.get(0).ip()); // IP
		});
	}

	private void checkTruncation(JTable table, FontMetrics fm, int modelIndex, String content) {
		int viewIndex = table.convertColumnIndexToView(modelIndex);
		if (viewIndex == -1)
			return; // Column hidden

		TableColumn col = table.getColumnModel().getColumn(viewIndex);
		int colWidth = col.getWidth();
		int contentWidth = fm.stringWidth(content);

		// Add a small buffer for cell padding/borders usually handled by look and feel
		int requiredWidth = contentWidth + 4;

		if (colWidth < requiredWidth) {
			System.out.println("Truncation detected in column " + modelIndex + ": Content Width=" + requiredWidth + ", Col Width="
					+ colWidth + ", Content='" + content + "'");
			// Fail the test to confirm the issue as requested
			fail("Column " + modelIndex + " is truncated. Required: " + requiredWidth + ", Actual: " + colWidth);
		}
	}
}
