package de.in.lsp.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.BiConsumer;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import de.in.lsp.model.LogEntry;

/**
 * Universal receiver for serialized log events from Logback, Log4j 1.x, and Log4j 2.x. Automatically detects the event type and converts it
 * to a LogEntry.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class UniversalSerializedReceiver extends LogStreamServer {

	public UniversalSerializedReceiver(int port, BiConsumer<LogEntry, SocketAddress> entryConsumer) {
		super(port, "Universal", entryConsumer);
	}

	@Override
	protected void handleConnection(Socket socket) {
		String clientIp = socket.getInetAddress().getHostAddress();
		SocketAddress remoteAddress = socket.getRemoteSocketAddress();
		int port = socket.getPort();
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))) {
			while (isRunning()) {
				Object obj = ois.readObject();
				LogEntry entry = null;

				if (obj instanceof ILoggingEvent event) {
					entry = convertLogback(event, clientIp, port);
				} else if (obj instanceof LoggingEvent event) {
					entry = convertLog4j1(event, clientIp, port);
				} else if (obj instanceof LogEvent event) {
					entry = convertLog4j2(event, clientIp, port);
				}

				if (entry != null) {
					publish(entry, remoteAddress);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			// Connection closed or invalid data
		} finally {
			publishDisconnect(remoteAddress);
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	private LogEntry convertLogback(ILoggingEvent event, String clientIp, int port) {
		LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());
		String appName = event.getMDCPropertyMap().get("application");
		if (appName == null || appName.isEmpty()) {
			appName = "RemoteApp";
		}

		String message = event.getFormattedMessage();
		if (event.getThrowableProxy() != null) {
			message += "\n" + ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(event.getThrowableProxy());
		}

		return new LogEntry(ts, event.getLevel().toString(), event.getThreadName(), event.getLoggerName(), clientIp, port,
				message, appName, message);
	}

	private LogEntry convertLog4j1(LoggingEvent event, String clientIp, int port) {
		LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());
		String appName = (String) event.getMDC("application");
		if (appName == null || appName.isEmpty()) {
			appName = "RemoteApp";
		}

		String message = event.getRenderedMessage();
		String[] throwableStr = event.getThrowableStrRep();
		if (throwableStr != null) {
			message += "\n" + String.join("\n", throwableStr);
		}

		return new LogEntry(ts, event.getLevel().toString(), event.getThreadName(), event.getLoggerName(), clientIp, port,
				message, appName, message);
	}

	private LogEntry convertLog4j2(LogEvent event, String clientIp, int port) {
		LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeMillis()), ZoneId.systemDefault());
		String appName = event.getContextData().getValue("application");
		if (appName == null || appName.isEmpty()) {
			appName = "RemoteApp";
		}

		String message = event.getMessage().getFormattedMessage();
		Throwable thrown = event.getThrown();
		if (thrown != null) {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			thrown.printStackTrace(pw);
			message += "\n" + sw.toString();
		}

		return new LogEntry(ts, event.getLevel().toString(), event.getThreadName(), event.getLoggerName(), clientIp, port,
				message, appName, message);
	}
}
