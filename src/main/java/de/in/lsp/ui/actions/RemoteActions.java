package de.in.lsp.ui.actions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.cuberact.swing.layout.Cell;
import org.cuberact.swing.layout.Composite;

import de.in.lsp.manager.LogManager;
import de.in.lsp.model.LogEntry;
import de.in.lsp.service.SshK8sService;
import de.in.lsp.ui.K8sPodSelectionDialog;
import de.in.lsp.ui.LogView;
import de.in.lsp.ui.ViewManager;
import de.in.lsp.util.LspLogger;

/**
 * Handles remote-related actions like SSH connection and K8s discovery.
 * Manages the connection workflow and log streaming from remote containers.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class RemoteActions {
    private final JFrame parentFrame;
    private final ViewManager viewManager;
    private static final String PREF_LAST_HOSTNAME = "last_hostname";

    public RemoteActions(JFrame parentFrame, ViewManager viewManager) {
        this.parentFrame = parentFrame;
        this.viewManager = viewManager;
    }

    private String getLastHostname() {
        return Preferences.userNodeForPackage(RemoteActions.class).get(PREF_LAST_HOSTNAME, "");
    }

    private void saveLastHostname(String host) {
        if (host != null && !host.isEmpty()) {
            Preferences.userNodeForPackage(RemoteActions.class).put(PREF_LAST_HOSTNAME, host);
        }
    }

    public void importFromK8s(LogView.LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
        Composite panel = new Composite();
        JTextField hostField = new JTextField(getLastHostname(), 20);
        JTextField userField = new JTextField("node-admin", 20);
        JPasswordField passField = new JPasswordField("", 20);

        panel.addCell(new JLabel("Host: ")).align(Cell.RIGHT);
        panel.addCell(hostField).width(400).rowEnd(true).padBottom(5);
        panel.addCell(new JLabel("User: ")).align(Cell.RIGHT);
        panel.addCell(userField).width(400).rowEnd(true).padBottom(5);
        panel.addCell(new JLabel("Password: ")).align(Cell.RIGHT);
        panel.addCell(passField).width(400);

        int result = JOptionPane.showConfirmDialog(parentFrame, panel, "SSH Connection", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION)
            return;

        performSshDiscovery(hostField.getText(), 22, userField.getText(), new String(passField.getPassword()), null,
                listener, columnVisibility);
    }

    public void importFromK8sAutomated(String credentials, String fetchFilter, LogView.LogViewListener listener,
            Map<Integer, Boolean> columnVisibility) {
        String host = "";
        String user = "node-admin";
        String password = "";

        // Format: [user[:password]@]host
        int atIndex = credentials.lastIndexOf('@');
        if (atIndex != -1) {
            host = credentials.substring(atIndex + 1);
            String userPass = credentials.substring(0, atIndex);
            int colonIndex = userPass.indexOf(':');
            if (colonIndex != -1) {
                user = userPass.substring(0, colonIndex);
                password = userPass.substring(colonIndex + 1);
            } else {
                user = userPass;
            }
        } else {
            host = credentials;
        }

        if (password.isEmpty() || host.isEmpty()) {
            showSshDialog(host, user, password, fetchFilter, listener, columnVisibility);
        } else {
            performSshDiscovery(host, 22, user, password, fetchFilter, listener, columnVisibility);
        }
    }

    private void showSshDialog(String host, String user, String password, String fetchFilter,
            LogView.LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
        Composite panel = new Composite();
        JTextField hostField = new JTextField(host.isEmpty() ? getLastHostname() : host, 30);
        JTextField userField = new JTextField(user, 30);
        JPasswordField passField = new JPasswordField(password, 30);

        panel.addCell(new JLabel("Host: ")).align(2);
        panel.addCell(hostField).width(400).rowEnd(true);
        panel.addCell(new JLabel("User: ")).align(2);
        panel.addCell(userField).width(400).rowEnd(true);
        panel.addCell(new JLabel("Password: ")).align(2);
        panel.addCell(passField).width(400);

        int result = JOptionPane.showConfirmDialog(parentFrame, panel, "SSH Connection", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION)
            return;

        performSshDiscovery(hostField.getText(), 22, userField.getText(), new String(passField.getPassword()),
                fetchFilter, listener, columnVisibility);
    }

    private void performSshDiscovery(String host, int port, String user, String password, String fetchFilter,
            LogView.LogViewListener listener, Map<Integer, Boolean> columnVisibility) {
        saveLastHostname(host);
        ProgressMonitor pm = new ProgressMonitor(parentFrame, "Connecting and discovering pods...", "", 0, 100);
        new Thread(() -> {
            try (SshK8sService sshService = new SshK8sService()) {
                sshService.connect(host, port, user, password);
                pm.setNote("Fetching pods...");
                var namespaces = sshService.discoverPods();

                final List<K8sPodSelectionDialog.SelectedContainer> selected = new ArrayList<>();
                if (fetchFilter != null && !fetchFilter.isEmpty()) {
                    selected.addAll(autoSelectContainers(namespaces, fetchFilter));
                }

                if (selected.isEmpty()) {
                    LspLogger.info("No containers selected via filter, showing selection dialog.");
                    SwingUtilities.invokeAndWait(() -> {
                        K8sPodSelectionDialog dialog = new K8sPodSelectionDialog(parentFrame, namespaces);
                        dialog.setVisible(true);
                        selected.addAll(dialog.getSelectedContainers());
                    });
                } else {
                    LspLogger.info("Auto-selected " + selected.size() + " containers via filter: " + fetchFilter);
                }

                if (selected.isEmpty())
                    return;

                for (var target : selected) {
                    pm.setNote("Streaming logs from " + target.pod + "...");
                    LspLogger.info(
                            "Streaming logs from " + target.namespace + "/" + target.pod + "/" + target.container);
                    try (InputStream is = sshService.streamLogs(target.namespace, target.pod, target.container)) {
                        LogManager lm = new LogManager();
                        String sourceName = target.pod + "_" + target.container + ".log";
                        List<LogEntry> entries = lm.parseStream(is, sourceName);
                        LspLogger.info("Finished streaming " + entries.size() + " entries from " + target.pod);
                        SwingUtilities.invokeLater(() -> {
                            viewManager.addLogView(entries, sourceName, columnVisibility, listener,
                                    LogView.ViewType.K8S);
                        });
                    } catch (Exception ex) {
                        LspLogger.error("Error streaming logs from " + target.pod, ex);
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(
                                () -> JOptionPane.showMessageDialog(parentFrame,
                                        "Error streaming " + target.pod + ": " + ex.getMessage()));
                    }
                }
            } catch (Exception ex) {
                LspLogger.error("K8s Discovery failed", ex);
                ex.printStackTrace();
                SwingUtilities.invokeLater(
                        () -> JOptionPane.showMessageDialog(parentFrame, "K8s Discovery failed: " + ex.getMessage()));
            } finally {
                pm.close();
            }
        }).start();
    }

    private List<K8sPodSelectionDialog.SelectedContainer> autoSelectContainers(
            List<SshK8sService.K8sNamespace> namespaces, String filter) {
        List<K8sPodSelectionDialog.SelectedContainer> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String[] patterns = filter.split(",");
        for (String pattern : patterns) {
            String p = pattern.trim();
            for (var ns : namespaces) {
                for (var pod : ns.getPods()) {
                    for (var container : pod.getContainers()) {
                        String fullPath = ns.getName() + "/" + pod.getName() + "/" + container;
                        if (matches(fullPath, p) && seen.add(fullPath)) {
                            result.add(new K8sPodSelectionDialog.SelectedContainer(ns.getName(), pod.getName(),
                                    container));
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean matches(String text, String pattern) {
        if (!pattern.contains("/")) {
            return text.contains(pattern);
        }
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return text.matches(regex);
    }
}
