package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.time.LocalDateTime;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.helper.WrapLayout;

/**
 * Tests for LogDetailView.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogDetailViewTest {

	@Test
	void testSetEntryPopulatesFields() {
		LogDetailView view = new LogDetailView(() -> {
		}, () -> {
		});
		LogEntry entry = new LogEntry(LocalDateTime.now(), "INFO", "main", "com.example.Logger", "127.0.0.1", "Test Message", "source.log",
				"raw line");

		view.setEntry(entry);

		// Access via reflection or component hierarchy traversal would be ideal, but for now let's check basic structure.

		// Structure: BorderLayout.NORTH -> Header Panel Header Panel: CENTER -> Meta Panel (WrapLayout)

		Component center = ((JPanel) view.getComponent(0)).getComponent(0); // Header -> Meta Panel
		assertTrue(center instanceof JPanel);
		JPanel metaPanel = (JPanel) center;
		assertTrue(metaPanel.getLayout() instanceof WrapLayout);

		// Meta Panel should have components since entry has data
		assertTrue(metaPanel.getComponentCount() > 0);

		// Find Message Area BorderLayout.CENTER -> JScrollPane -> Viewport -> JTextArea
		Component scrollPane = view.getComponent(1);
		assertTrue(scrollPane instanceof JScrollPane);
		Component viewport = ((JScrollPane) scrollPane).getViewport().getView();
		assertTrue(viewport instanceof JTextArea);
		JTextArea messageArea = (JTextArea) viewport;

		assertEquals("Test Message", messageArea.getText());
		assertTrue(messageArea.getLineWrap());
		assertTrue(messageArea.getWrapStyleWord());

		// Verify IP field is present
		boolean ipFound = false;
		for (Component c : metaPanel.getComponents()) {
			if (c instanceof JTextField) {
				JTextField tf = (JTextField) c;
				if (tf.getBorder() instanceof javax.swing.border.CompoundBorder) {
					javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) tf.getBorder();
					if (cb.getOutsideBorder() instanceof javax.swing.border.TitledBorder) {
						javax.swing.border.TitledBorder tb = (javax.swing.border.TitledBorder) cb.getOutsideBorder();
						if ("IP".equals(tb.getTitle()) && "127.0.0.1".equals(tf.getText())) {
							ipFound = true;
						}
					}
				}
			}
		}
		assertTrue(ipFound, "IP field should be present with correct value and TitledBorder");
	}

	@Test
	void testSetFontSizeUpdatesFont() {
		LogDetailView view = new LogDetailView(() -> {
		}, () -> {
		});
		LogEntry entry = new LogEntry(LocalDateTime.now(), "INFO", "main", "com.example.Logger", "127.0.0.1", "Message", "source.log",
				"raw");
		view.setEntry(entry);

		int newSize = 20;
		view.setFontSize(newSize);

		// check message area
		JTextArea messageArea = (JTextArea) ((JScrollPane) view.getComponent(1)).getViewport().getView();
		assertEquals(newSize, messageArea.getFont().getSize());

		// check fields
		JPanel header = (JPanel) view.getComponent(0);
		JPanel metaPanel = (JPanel) header.getComponent(0);

		for (Component c : metaPanel.getComponents()) {
			if (c instanceof JTextField) {
				JTextField tf = (JTextField) c;
				assertEquals(newSize, tf.getFont().getSize());
			}
		}
	}

	@Test
	void testSetEntryNullClearsFields() {
		LogDetailView view = new LogDetailView(() -> {
		}, () -> {
		});
		view.setEntry(null);

		// Header Panel -> Meta Panel
		JPanel metaPanel = (JPanel) ((JPanel) view.getComponent(0)).getComponent(0);

		assertEquals(0, metaPanel.getComponentCount());

		JTextArea messageArea = (JTextArea) ((JScrollPane) view.getComponent(1)).getViewport().getView();
		assertEquals("", messageArea.getText());
	}

	@Test
	void testSetEntryRespectsCurrentFontSize() {
		LogDetailView view = new LogDetailView(() -> {
		}, () -> {
		});

		// 1. Set font size FIRST
		int customSize = 24;
		view.setFontSize(customSize);

		// 2. Set entry SECOND (which triggers addField)
		LogEntry entry = new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg", "src", "raw");
		view.setEntry(entry);

		// 3. Verify fields have the custom size
		JPanel header = (JPanel) view.getComponent(0);
		JPanel metaPanel = (JPanel) header.getComponent(0);

		boolean fieldFound = false;
		for (Component c : metaPanel.getComponents()) {
			if (c instanceof JTextField) {
				fieldFound = true;
				JTextField tf = (JTextField) c;
				assertEquals(customSize, tf.getFont().getSize(), "Field font size should match setFontSize");

				// Check border font too
				if (tf.getBorder() instanceof javax.swing.border.CompoundBorder) {
					javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) tf.getBorder();
					if (cb.getOutsideBorder() instanceof javax.swing.border.TitledBorder) {
						javax.swing.border.TitledBorder tb = (javax.swing.border.TitledBorder) cb.getOutsideBorder();
						assertEquals(customSize, tb.getTitleFont().getSize(), "Border title font size should match");
					}
				}
			}
		}
		assertTrue(fieldFound, "Should have created fields");
	}

	@Test
	void testLongSourceFieldNotTruncated() {
		LogDetailView view = new LogDetailView(() -> {
		}, () -> {
		});
		String longSource = "a-very-long-source-file-name-that-exceeds-thirty-characters-and-should-not-be-truncated.log";
		LogEntry entry = new LogEntry(LocalDateTime.now(), "INFO", "main", "Logger", "127.0.0.1", "Msg", longSource, "raw");
		view.setEntry(entry);

		JPanel header = (JPanel) view.getComponent(0);
		JPanel metaPanel = (JPanel) header.getComponent(0);

		for (Component c : metaPanel.getComponents()) {
			if (c instanceof JTextField) {
				JTextField tf = (JTextField) c;
				if (tf.getText().equals(longSource)) {
					// Check that columns were set high enough
					assertTrue(tf.getColumns() > 30, "Columns should be set > 30 for long text");
					assertTrue(tf.getColumns() >= longSource.length(), "Columns should accommodate full length");
				}
			}
		}
	}
}
