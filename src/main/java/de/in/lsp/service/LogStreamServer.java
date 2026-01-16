package de.in.lsp.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import de.in.lsp.model.LogEntry;

/**
 * Base class for log stream servers.
 * Handles TCP connections and delegates parsing to subclasses.
 * Supports concurrent connections via buffered input streams.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public abstract class LogStreamServer implements Runnable {

    private final int port;
    private final String protocol;
    private final BiConsumer<LogEntry, SocketAddress> entryConsumer;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    protected LogStreamServer(int port, String protocol, BiConsumer<LogEntry, SocketAddress> entryConsumer) {
        this.port = port;
        this.protocol = protocol;
        this.entryConsumer = entryConsumer;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        new Thread(this, "LogStreamServer-" + protocol + "-" + port).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        executor.shutdownNow();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                executor.execute(() -> handleConnection(socket));
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected abstract void handleConnection(Socket socket);

    protected void publish(LogEntry entry, SocketAddress remoteAddress) {
        entryConsumer.accept(entry, remoteAddress);
    }

    protected void publishDisconnect(SocketAddress remoteAddress) {
        entryConsumer.accept(null, remoteAddress);
    }
}
