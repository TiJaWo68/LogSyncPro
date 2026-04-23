package de.in.lsp.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDesktopPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.in.lsp.model.LogEntry;

public class ViewManagerGroupingTest {

	private ViewManager viewManager;
	private JDesktopPane desktopPane;

	@BeforeEach
	public void setUp() {
		desktopPane = new JDesktopPane();
		viewManager = new ViewManager(desktopPane, v -> {}, () -> {});
	}

	@Test
	public void testGroupingByPortForRemoteApp() throws Exception {
		SocketAddress addr1 = new InetSocketAddress("127.0.0.1", 12345);
		LogEntry entry1 = new LogEntry(LocalDateTime.now(), "INFO", "main", "logger1", "127.0.0.1", 12345, "msg1", "RemoteApp", "msg1");
		LogEntry entry2 = new LogEntry(LocalDateTime.now(), "INFO", "main", "logger2", "127.0.0.1", 12345, "msg2", "RemoteApp", "msg2");

		Map<Integer, Boolean> visibility = new HashMap<>();

		// Process first entry
		viewManager.handleStreamingEntry(entry1, addr1, mock(LogViewListener.class), visibility);

		// Wait for invokeLater
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {}); // Second one for the nested invokeLater in handleStreamingEntry

		assertEquals(1, viewManager.getLogViews().size(), "Should have created exactly one LogView");
		LogView view = viewManager.getLogViews().get(0);
		assertEquals("Remote(127.0.0.1:12345)", view.getBaseTitle());

		// Process second entry from DIFFERENT logger but SAME port
		viewManager.handleStreamingEntry(entry2, addr1, mock(LogViewListener.class), visibility);
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});

		assertEquals(1, viewManager.getLogViews().size(), "Should still have only one LogView");
		assertEquals(2, view.getEntries().size(), "Both entries should be in the same view");
	}

	@Test
	public void testSeparationByPortForRemoteApp() throws Exception {
		SocketAddress addr1 = new InetSocketAddress("127.0.0.1", 12345);
		SocketAddress addr2 = new InetSocketAddress("127.0.0.1", 54321);
		LogEntry entry1 = new LogEntry(LocalDateTime.now(), "INFO", "main", "logger1", "127.0.0.1", 12345, "msg1", "RemoteApp", "msg1");
		LogEntry entry2 = new LogEntry(LocalDateTime.now(), "INFO", "main", "logger1", "127.0.0.1", 54321, "msg2", "RemoteApp", "msg2");

		Map<Integer, Boolean> visibility = new HashMap<>();

		viewManager.handleStreamingEntry(entry1, addr1, mock(LogViewListener.class), visibility);
		viewManager.handleStreamingEntry(entry2, addr2, mock(LogViewListener.class), visibility);

		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});

		assertEquals(2, viewManager.getLogViews().size(), "Should have created two LogViews due to different ports");
	}
}
