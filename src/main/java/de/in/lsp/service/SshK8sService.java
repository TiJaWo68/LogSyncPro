package de.in.lsp.service;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Service for SSH communication and Kubernetes log collection.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class SshK8sService implements AutoCloseable {

    private SshClient client;
    private ClientSession session;
    private String password;
    private static final long TIMEOUT = 10000L;

    public void connect(String host, int port, String user, String password) throws IOException {
        this.password = password;
        client = SshClient.setUpDefaultClient();
        client.start();
        session = client.connect(user, host, port).verify(TIMEOUT).getSession();
        session.addPasswordIdentity(password);
        session.auth().verify(TIMEOUT);
    }

    public List<K8sNamespace> discoverPods() throws IOException {
        // Use sudo -S -i to ensure root environment is used, and feed password via
        // stdin
        String command = "sudo -S -i kubectl get pods -A -o jsonpath='{range .items[]}{.metadata.namespace}{\"\\t\"}{.metadata.name}{\"\\t\"}{range .spec.containers[]}{.name}{\" \"}{end}{\"\\n\"}{end}'";
        String output = executeCommand(command);
        return parseK8sOutput(output);
    }

    private String executeCommand(String command) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                ClientChannel channel = session.createExecChannel(command)) {
            channel.setOut(out);
            channel.setErr(err);
            channel.open().verify(TIMEOUT);

            if (command.contains("sudo -S")) {
                try (var os = channel.getInvertedIn()) {
                    os.write((password + "\n").getBytes());
                    os.flush();
                }
            }

            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);

            String errorOutput = err.toString().trim();
            if (!errorOutput.isEmpty() && out.size() == 0) {
                if (!errorOutput.toLowerCase().contains("password for")) {
                    throw new IOException("Remote command failed: " + errorOutput);
                }
            }
            return out.toString();
        }
    }

    public InputStream streamLogs(String namespace, String pod, String container) throws IOException {
        String command = String.format("sudo -S -i kubectl -n %s logs %s -c %s", namespace, pod, container);
        ClientChannel channel = session.createExecChannel(command);
        channel.open().verify(TIMEOUT);

        try (var os = channel.getInvertedIn()) {
            os.write((password + "\n").getBytes());
            os.flush();
        }

        return channel.getInvertedOut();
    }

    private List<K8sNamespace> parseK8sOutput(String output) {
        Map<String, K8sNamespace> namespaces = new TreeMap<>();
        if (output == null || output.trim().isEmpty())
            return new ArrayList<>();

        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // Try splitting by tab, then by multiple spaces as fallback
            String[] parts = line.split("\\t");
            if (parts.length < 2) {
                parts = line.split("\\s{2,}"); // Split by 2+ spaces
            }

            if (parts.length >= 2) {
                String nsName = parts[0].trim();
                String podName = parts[1].trim();
                String containersStr = parts.length > 2 ? parts[2].trim() : "";
                List<String> containers = Arrays.asList(containersStr.split("\\s+"));

                K8sNamespace ns = namespaces.computeIfAbsent(nsName, K8sNamespace::new);
                ns.addPod(new K8sPod(podName, containers));
            }
        }
        return new ArrayList<>(namespaces.values());
    }

    @Override
    public void close() throws Exception {
        if (session != null)
            session.close();
        if (client != null)
            client.stop();
    }

    public static class K8sNamespace {
        private final String name;
        private final List<K8sPod> pods = new ArrayList<>();

        public K8sNamespace(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<K8sPod> getPods() {
            return pods;
        }

        public void addPod(K8sPod pod) {
            pods.add(pod);
        }
    }

    public static class K8sPod {
        private final String name;
        private final List<String> containers;

        public K8sPod(String name, List<String> containers) {
            this.name = name;
            this.containers = containers;
        }

        public String getName() {
            return name;
        }

        public List<String> getContainers() {
            return containers;
        }
    }
}
