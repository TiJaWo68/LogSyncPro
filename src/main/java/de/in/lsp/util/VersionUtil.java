package de.in.lsp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for version handling.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class VersionUtil {

	/**
	 * Retrieves the version of a Maven artifact from its generated pom.properties file.
	 * 
	 * @param groupId    The group ID of the artifact.
	 * @param artifactId The artifact ID of the artifact.
	 * @return The version string, or "unknown" if not found.
	 */
	public static String retrieveVersionFromPom(String groupId, String artifactId) {
		try (InputStream inpt = ClassLoader.getSystemResourceAsStream("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties")) {
			Properties p = new Properties();
			if (inpt != null) {
				p.load(inpt);
				return p.getProperty("version");
			}
		} catch (IOException ex) {
			LspLogger.warn("Failed to load version for " + groupId + ":" + artifactId + ": " + ex.getMessage());
		}
		return "unknown";
	}
}
