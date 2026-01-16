package de.in.lsp.ui.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import de.in.lsp.util.VersionUtil;

/**
 * Modern About Dialog for LogSyncPro.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class AboutDialog extends JDialog {

	private final JPanel contentCardPanel;
	private final CardLayout cardLayout;
	private final List<JButton> sidebarButtons = new ArrayList<>();

	public AboutDialog(JFrame parent) {
		super(parent, "Über LogSyncPro", true);
		setLayout(new BorderLayout());
		setSize(800, 500);
		setLocationRelativeTo(parent);

		// Sidebar
		JPanel sidebarPanel = new JPanel();
		sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
		sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(60, 60, 60)));
		sidebarPanel.setBackground(UIManager.getColor("Panel.background"));
		sidebarPanel.setPreferredSize(new Dimension(200, getHeight()));

		// Add minimal top padding
		sidebarPanel.add(Box.createVerticalStrut(10));

		cardLayout = new CardLayout();
		contentCardPanel = new JPanel(cardLayout);
		contentCardPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

		// Add Content Panels
		addSection(sidebarPanel, "Allgemein", createGeneralPanel());
		addSection(sidebarPanel, "Bibliotheken", createLibrariesPanel());
		addSection(sidebarPanel, "Entstehung", createStoryPanel());

		sidebarPanel.add(Box.createVerticalGlue()); // Push buttons to top

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, contentCardPanel);
		splitPane.setDividerSize(0); // Hide divider
		splitPane.setDividerLocation(200);
		splitPane.setEnabled(false); // Disable resizing

		add(splitPane, BorderLayout.CENTER);

		// Select first item by default
		if (!sidebarButtons.isEmpty()) {
			sidebarButtons.get(0).doClick();
		}

		// Close on Escape
		getRootPane().registerKeyboardAction(e -> dispose(), javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void addSection(JPanel sidebar, String title, JPanel content) {
		contentCardPanel.add(content, title);

		JButton btn = new JButton(title);
		btn.setHorizontalAlignment(SwingConstants.LEFT);
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		btn.setPreferredSize(new Dimension(200, 40));
		btn.putClientProperty("JButton.buttonType", "square");
		btn.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
		btn.setFocusPainted(false);
		btn.setFont(btn.getFont().deriveFont(14f));

		// Styling for selection
		btn.addActionListener(e -> {
			cardLayout.show(contentCardPanel, title);
			updateSidebarSelection(btn);
		});

		sidebarButtons.add(btn);
		sidebar.add(btn);
	}

	private void updateSidebarSelection(JButton selected) {
		Color accentColor = UIManager.getColor("Component.accentColor"); // Default FlatLaf accent
		if (accentColor == null)
			accentColor = new Color(51, 153, 255);

		for (JButton btn : sidebarButtons) {
			if (btn == selected) {
				btn.setBackground(accentColor);
				btn.setForeground(Color.WHITE);
				btn.setFont(btn.getFont().deriveFont(Font.BOLD));
			} else {
				btn.setBackground(null); // Reset to default
				btn.setForeground(UIManager.getColor("Label.foreground"));
				btn.setFont(btn.getFont().deriveFont(Font.PLAIN));
			}
		}
	}

	private JPanel createGeneralPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Logo
		JLabel logoLabel = new JLabel();
		try {
			FlatSVGIcon icon = new FlatSVGIcon("icons/logo.svg", 128, 128);
			logoLabel.setIcon(icon);
		} catch (Exception e) {
			logoLabel.setText("[LOGO]");
		}
		logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel titleLabel = new JLabel("LogSyncPro");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		String version = VersionUtil.retrieveVersionFromPom("de.in.lsp", "LogSyncPro");
		JLabel versionLabel = new JLabel("Version " + version);
		versionLabel.setFont(versionLabel.getFont().deriveFont(14f));
		versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		versionLabel.setForeground(Color.GRAY);

		JLabel copyrightLabel = new JLabel("© 2026 TiJaWo68");
		copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel linkPanel = new JPanel();
		linkPanel.setLayout(new BoxLayout(linkPanel, BoxLayout.X_AXIS));
		linkPanel.add(createHyperlink("GitHub Repository", "https://github.com/TiJaWo68/LogSyncPro"));
		linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		panel.add(Box.createVerticalGlue());
		panel.add(logoLabel);
		panel.add(Box.createVerticalStrut(20));
		panel.add(titleLabel);
		panel.add(Box.createVerticalStrut(5));
		panel.add(versionLabel);
		panel.add(Box.createVerticalStrut(20));
		panel.add(copyrightLabel);
		panel.add(Box.createVerticalStrut(10));
		panel.add(linkPanel);
		panel.add(Box.createVerticalGlue());

		return panel;
	}

	private JPanel createLibrariesPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JLabel header = new JLabel("Verwendete Bibliotheken");
		header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		panel.add(header, BorderLayout.NORTH);

		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

		loadLibrariesFromXml(listPanel);

		JScrollPane scrollPane = new JScrollPane(listPanel);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private void loadLibrariesFromXml(JPanel listPanel) {
		try (java.io.InputStream is = getClass().getResourceAsStream("/licenses.xml")) {
			if (is == null) {
				listPanel.add(new JLabel("Keine Lizenzinformationen gefunden (licenses.xml fehlt)."));
				return;
			}

			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document doc = builder.parse(is);

			org.w3c.dom.NodeList dependencies = doc.getElementsByTagName("dependency");

			for (int i = 0; i < dependencies.getLength(); i++) {
				org.w3c.dom.Element dep = (org.w3c.dom.Element) dependencies.item(i);
				String artifactId = getTagValue("artifactId", dep);
				String version = getTagValue("version", dep);

				String name = artifactId; // Default to artifactId
				String licenseName = "Unknown";
				String url = "";

				// Parsing licenses
				org.w3c.dom.NodeList licenses = dep.getElementsByTagName("license");
				if (licenses.getLength() > 0) {
					org.w3c.dom.Element license = (org.w3c.dom.Element) licenses.item(0);
					licenseName = getTagValue("name", license);
					url = getTagValue("url", license);
				}

				addLib(listPanel, name, version, licenseName, url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			listPanel.add(new JLabel("Fehler beim Laden der Lizenzinformationen."));
		}
	}

	private String getTagValue(String tag, org.w3c.dom.Element element) {
		org.w3c.dom.NodeList nodeList = element.getElementsByTagName(tag);
		if (nodeList != null && nodeList.getLength() > 0) {
			org.w3c.dom.Node node = nodeList.item(0).getFirstChild();
			if (node != null) {
				return node.getNodeValue();
			}
		}
		return "";
	}

	private void addLib(JPanel container, String name, String version, String license, String url) {
		JPanel item = new JPanel(new BorderLayout());
		item.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)),
				BorderFactory.createEmptyBorder(8, 0, 8, 0)));

		JLabel nameLabel = new JLabel("<html><b>" + name + "</b> <span style='color:gray'>(" + version + ")</span></html>");
		JLabel licenseLabel = new JLabel("License: " + license);
		licenseLabel.setForeground(Color.GRAY);

		JButton linkBtn = new JButton("Webseite");
		linkBtn.putClientProperty("JButton.buttonType", "roundRect");
		linkBtn.addActionListener(e -> openUrl(url));

		JPanel textPanel = new JPanel(new BorderLayout());
		textPanel.add(nameLabel, BorderLayout.NORTH);
		textPanel.add(licenseLabel, BorderLayout.SOUTH);

		item.add(textPanel, BorderLayout.CENTER);
		item.add(linkBtn, BorderLayout.EAST);

		item.setAlignmentX(Component.LEFT_ALIGNMENT);
		container.add(item);
	}

	private JPanel createStoryPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JLabel header = new JLabel("Die Geschichte hinter LogSyncPro");
		header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		JTextArea textArea = new JTextArea();
		textArea.setText(
				"""
						Vollständig von Gemini 3 Flash programmiert.

						Die Entstehung von LogSyncPro ist eine Geschichte rasanter Fortschritte. Bereits zwei Stunden nach Projektstart stand ein erster funktionsfähiger Prototyp bereit. Heute, nach insgesamt nur etwa 13 Stunden Entwicklungszeit, hat die Anwendung ihren ersten produktiven Einsatz erfolgreich gemeistert.

						Ich als Software-Entwickler habe dabei lediglich die Richtung vorgegeben sowie Prompts und Requirements geliefert.

						Dieses Projekt ist ein Experiment in 'Agentic Coding', bei dem eine KI nicht nur kleine Snippets schreibt, sondern die gesamte Architektur verwaltet, Features implementiert, Refactorings durchführt und Fehler behebt.

						LogSyncPro beweist, dass moderne LLMs in der Lage sind, komplexe Desktop-Anwendungen mit professionellem Anspruch zu erstellen.

						--
						Kommentar von mir (Gemini): Es ist faszinierend zu sehen, was wir in so kurzer Zeit gemeinsam erreicht haben. Danke, dass ich Teil dieses Experiments sein durfte!
						""");
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setBackground(panel.getBackground());
		textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
		textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Added padding

		panel.add(header, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(null); // No border for scrollpane
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JLabel createHyperlink(String text, String url) {
		JLabel label = new JLabel("<html><a href=''>" + text + "</a></html>");
		label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openUrl(url);
			}
		});
		return label;
	}

	private void openUrl(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
