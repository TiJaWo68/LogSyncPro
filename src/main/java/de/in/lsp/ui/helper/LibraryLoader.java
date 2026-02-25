package de.in.lsp.ui.helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.in.lsp.util.LspLogger;

/**
 * Loads library information from licenses.xml.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LibraryLoader {

	public record LibraryInfo(String groupId, String artifactId, String version, String license, String licenseUrl,
			String homepageUrl) {
	}

	/**
	 * Known homepage URLs for libraries. Key is "groupId:artifactId".
	 */
	private static final Map<String, String> KNOWN_HOMEPAGES = Map.ofEntries(
			Map.entry("ch.qos.logback:logback-classic", "https://github.com/qos-ch/logback"),
			Map.entry("ch.qos.logback:logback-core", "https://github.com/qos-ch/logback"),
			Map.entry("com.formdev:flatlaf", "https://github.com/JFormDesigner/FlatLaf"),
			Map.entry("com.formdev:flatlaf-extras", "https://github.com/JFormDesigner/FlatLaf"),
			Map.entry("com.github.weisj:jsvg", "https://github.com/weisJ/jsvg"),
			Map.entry("com.miglayout:miglayout-core", "https://github.com/mikaelgrev/miglayout"),
			Map.entry("com.miglayout:miglayout-swing", "https://github.com/mikaelgrev/miglayout"),
			Map.entry("commons-codec:commons-codec", "https://commons.apache.org/proper/commons-codec/"),
			Map.entry("commons-io:commons-io", "https://commons.apache.org/proper/commons-io/"),
			Map.entry("de.in:simpleupdraft4j", "https://github.com/TiJaWo68/simpleupdraft4j"),
			Map.entry("log4j:log4j", "https://logging.apache.org/log4j/1.x/"),
			Map.entry("org.apache.commons:commons-compress", "https://commons.apache.org/proper/commons-compress/"),
			Map.entry("org.apache.commons:commons-lang3", "https://commons.apache.org/proper/commons-lang/"),
			Map.entry("org.apache.logging.log4j:log4j-api", "https://logging.apache.org/log4j/2.x/"),
			Map.entry("org.apache.logging.log4j:log4j-core", "https://logging.apache.org/log4j/2.x/"),
			Map.entry("org.apache.sshd:sshd-common", "https://github.com/apache/mina-sshd"),
			Map.entry("org.apache.sshd:sshd-core", "https://github.com/apache/mina-sshd"),
			Map.entry("org.apiguardian:apiguardian-api", "https://github.com/apiguardian-team/apiguardian"),
			Map.entry("org.cuberact:cuberact-swing-layout", "https://github.com/nicenemo/cubeern-swing-layout"),
			Map.entry("org.junit.jupiter:junit-jupiter-api", "https://junit.org/junit5/"),
			Map.entry("org.junit.jupiter:junit-jupiter-engine", "https://junit.org/junit5/"),
			Map.entry("org.junit.platform:junit-platform-commons", "https://junit.org/junit5/"),
			Map.entry("org.junit.platform:junit-platform-engine", "https://junit.org/junit5/"),
			Map.entry("org.opentest4j:opentest4j", "https://github.com/ota4j-team/opentest4j"),
			Map.entry("org.slf4j:jcl-over-slf4j", "https://www.slf4j.org/"),
			Map.entry("org.slf4j:slf4j-api", "https://www.slf4j.org/"),
			Map.entry("org.tukaani:xz", "https://tukaani.org/xz/java.html"));

	public List<LibraryInfo> loadLibraries() {
		return loadLibraries(getClass().getResourceAsStream("/licenses.xml"));
	}

	/**
	 * Loads libraries from the given InputStream (for testability).
	 */
	public List<LibraryInfo> loadLibraries(InputStream is) {
		List<LibraryInfo> libs = new ArrayList<>();
		if (is == null) {
			return libs;
		}

		try (is) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("dependency");

			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					String groupId = getDirectChildValue(eElement, "groupId");
					String artifactId = getDirectChildValue(eElement, "artifactId");
					String version = getDirectChildValue(eElement, "version");

					// License info from nested <license> element
					String licenseName = "";
					String licenseUrl = "";
					NodeList licensesNodes = eElement.getElementsByTagName("license");
					if (licensesNodes.getLength() > 0) {
						Element lElement = (Element) licensesNodes.item(0);
						licenseName = getDirectChildValue(lElement, "name");
						licenseUrl = getDirectChildValue(lElement, "url");
					}

					String homepageUrl = resolveHomepageUrl(groupId, artifactId);

					libs.add(new LibraryInfo(groupId, artifactId, version, licenseName, licenseUrl, homepageUrl));
				}
			}
		} catch (Exception e) {
			LspLogger.error("Failed to load licenses.xml", e);
		}
		return libs;
	}

	/**
	 * Resolves the homepage URL for a library. Uses known mappings first, then
	 * falls
	 * back to a Maven Central search URL.
	 */
	static String resolveHomepageUrl(String groupId, String artifactId) {
		String key = groupId + ":" + artifactId;
		String known = KNOWN_HOMEPAGES.get(key);
		if (known != null) {
			return known;
		}
		return "https://mvnrepository.com/artifact/" + groupId + "/" + artifactId;
	}

	/**
	 * Gets the text value of a direct child element (non-recursive).
	 */
	static String getDirectChildValue(Element parent, String tagName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
				return child.getTextContent().trim();
			}
		}
		return "";
	}
}
