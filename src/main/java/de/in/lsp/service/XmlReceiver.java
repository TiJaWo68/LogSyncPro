package de.in.lsp.service;

import de.in.lsp.model.LogEntry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Receiver for XML-formatted log events (Log4j/Logback XMLLayout).
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class XmlReceiver extends LogStreamServer {

    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "<log4j:event.*?logger=\"(.*?)\".*?timestamp=\"(.*?)\".*?level=\"(.*?)\".*?thread=\"(.*?)\".*?>",
            Pattern.DOTALL);
    private static final Pattern MESSAGE_PATTERN = Pattern
            .compile("<log4j:message><!\\[CDATA\\[(.*?)\\]\\]></log4j:message>", Pattern.DOTALL);

    public XmlReceiver(int port, BiConsumer<LogEntry, SocketAddress> entryConsumer) {
        super(port, "XML", entryConsumer);
    }

    @Override
    protected void handleConnection(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        SocketAddress remoteAddress = socket.getRemoteSocketAddress();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            StringBuilder buffer = new StringBuilder();
            String line;
            while (isRunning() && (line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
                if (line.contains("</log4j:event>")) {
                    parseAndPublish(buffer.toString(), clientIp, remoteAddress);
                    buffer.setLength(0);
                }
            }
        } catch (IOException e) {
            // Connection closed
        } finally {
            publishDisconnect(remoteAddress);
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void parseAndPublish(String xml, String clientIp, SocketAddress remoteAddress) {
        Matcher eventMatcher = EVENT_PATTERN.matcher(xml);
        if (eventMatcher.find()) {
            String logger = eventMatcher.group(1);
            long timestamp = Long.parseLong(eventMatcher.group(2));
            String level = eventMatcher.group(3);
            String thread = eventMatcher.group(4);

            String message = "";
            Matcher msgMatcher = MESSAGE_PATTERN.matcher(xml);
            if (msgMatcher.find()) {
                message = msgMatcher.group(1);
            }

            LocalDateTime ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

            publish(new LogEntry(
                    ts,
                    level,
                    thread,
                    logger,
                    clientIp,
                    message,
                    "RemoteApp",
                    xml), remoteAddress);
        }
    }
}
