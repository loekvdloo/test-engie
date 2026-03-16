package nl.engie.allocation.pipeline;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Utility for safe XML parsing and namespace-agnostic tag extraction.
 */
public final class XmlFieldExtractor {

    private XmlFieldExtractor() {
    }

    public static Optional<Document> parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return Optional.empty();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return Optional.of(builder.parse(new InputSource(new StringReader(xml))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String getRootLocalName(Document document) {
        if (document == null || document.getDocumentElement() == null) {
            return null;
        }
        String local = document.getDocumentElement().getLocalName();
        if (local != null && !local.isBlank()) {
            return local;
        }
        return normalizeName(document.getDocumentElement().getNodeName());
    }

    public static String getFirstText(Document document, String... names) {
        if (document == null || names == null || names.length == 0) {
            return null;
        }
        for (String name : names) {
            String normalized = normalizeName(name);
            NodeList nodeList = document.getElementsByTagName("*");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element element) {
                    String local = element.getLocalName();
                    String nodeName = element.getNodeName();
                    if (normalized.equalsIgnoreCase(normalizeName(local))
                            || normalized.equalsIgnoreCase(normalizeName(nodeName))) {
                        String text = element.getTextContent();
                        if (text != null && !text.isBlank()) {
                            return text.trim();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<String> getAllTexts(Document document, String... names) {
        List<String> values = new ArrayList<>();
        if (document == null || names == null || names.length == 0) {
            return values;
        }

        List<String> normalizedNames = new ArrayList<>();
        for (String name : names) {
            normalizedNames.add(normalizeName(name));
        }

        NodeList nodeList = document.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element element) {
                String local = normalizeName(element.getLocalName());
                String nodeName = normalizeName(element.getNodeName());
                boolean match = normalizedNames.stream()
                        .anyMatch(n -> n.equalsIgnoreCase(local) || n.equalsIgnoreCase(nodeName));
                if (match) {
                    String text = element.getTextContent();
                    if (text != null && !text.isBlank()) {
                        values.add(text.trim());
                    }
                }
            }
        }

        return values;
    }

    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutPrefix = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        return withoutPrefix.replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
    }
}
