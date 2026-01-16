package de.in.lsp.ui.helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    public record LibraryInfo(String name, String version, String license, String url) {
    }

    public List<LibraryInfo> loadLibraries() {
        List<LibraryInfo> libs = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/licenses.xml")) {
            if (is == null) {
                return libs; // No licenses.xml found
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("dependency");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String name = getTagValue("name", eElement);
                    if (name.isEmpty())
                        name = getTagValue("artifactId", eElement); // Fallback

                    String version = getTagValue("version", eElement);
                    String url = getTagValue("url", eElement);

                    // License
                    String licenseName = "";
                    NodeList licenses = eElement.getElementsByTagName("license");
                    if (licenses.getLength() > 0) {
                        Element lElement = (Element) licenses.item(0);
                        licenseName = getTagValue("name", lElement);
                    }

                    libs.add(new LibraryInfo(name, version, licenseName, url));
                }
            }
        } catch (Exception e) {
            LspLogger.error("Failed to load licenses.xml", e);
        }
        return libs;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nlList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue == null ? "" : nValue.getNodeValue().trim();
    }
}
