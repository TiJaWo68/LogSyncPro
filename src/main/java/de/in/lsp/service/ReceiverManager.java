package de.in.lsp.service;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.in.lsp.model.LogEntry;
import de.in.lsp.ui.LogViewListener;
import de.in.lsp.ui.ViewManager;
import de.in.lsp.util.LspLogger;

/**
 * Manages the lifecycle of log stream receivers. Handles starting, stopping, and routing incoming entries to views.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class ReceiverManager {

	private final List<LogStreamServer> receivers = new ArrayList<>();
	private final ViewManager viewManager;
	private final LogViewListener listener;
	private final Map<Integer, Boolean> columnVisibility;

	public ReceiverManager(ViewManager viewManager, LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
		this.viewManager = viewManager;
		this.listener = listener;
		this.columnVisibility = columnVisibility;
		initReceivers();
	}

	private void initReceivers() {
		receivers.add(new UniversalSerializedReceiver(4445, this::handleIncomingLogEntry));
		receivers.add(new UniversalSerializedReceiver(4444, this::handleIncomingLogEntry));
		receivers.add(new UniversalSerializedReceiver(4560, this::handleIncomingLogEntry));
		receivers.add(new XmlReceiver(4561, this::handleIncomingLogEntry));
		// Port 10000/10001 (Lilith Multiplex) could be added here later
	}

	public void handleIncomingLogEntry(LogEntry entry, SocketAddress remoteAddress) {
		if (entry == null) {
			LspLogger.info("Log stream connection closed from " + remoteAddress);
		}
		viewManager.handleStreamingEntry(entry, remoteAddress, listener, columnVisibility);
	}

	public List<LogStreamServer> getReceivers() {
		return receivers;
	}

	public void startReceiver(int port) throws IOException {
		for (LogStreamServer r : receivers) {
			if (r.getPort() == port) {
				if (!r.isRunning()) {
					LspLogger.info("Starting receiver " + r.getProtocol() + " on port " + port);
					r.start();
				}
				return;
			}
		}
	}

	public void stopReceiver(int port) {
		for (LogStreamServer r : receivers) {
			if (r.getPort() == port) {
				LspLogger.info("Stopping receiver " + r.getProtocol() + " on port " + port);
				r.stop();
				return;
			}
		}
	}

	public void stopAll() {
		for (LogStreamServer r : receivers) {
			r.stop();
		}
	}
}
