package nl.engie.allocation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility class voor input sanitisatie en validatie.
 *
 * <p>Beschermt tegen:
 * <ul>
 *   <li>Path traversal in UUID parameters</li>
 *   <li>SQL injection via EAN codes</li>
 *   <li>Oversized XML payloads</li>
 * </ul>
 */
public final class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    /** UUID formaat: 8-4-4-4-12 hexadecimale tekens */
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** EAN-code: 13 of 18 cijfers */
    private static final Pattern EAN_PATTERN = Pattern.compile("^\\d{13}(\\d{5})?$");

    /** Maximale XML grootte: 2 MB */
    public static final int MAX_XML_SIZE = 2 * 1024 * 1024;

    private InputSanitizer() {}

    /**
     * Valideer of een string een geldig UUID is.
     * Voorkomt path traversal en injection via UUID parameters.
     */
    public static boolean isValidUuid(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Valideer of een EAN-code het juiste formaat heeft (13 of 18 cijfers).
     * Null is toegestaan (optioneel veld).
     */
    public static boolean isValidEanCode(String eanCode) {
        if (eanCode == null || eanCode.isBlank()) return true; // optioneel
        return EAN_PATTERN.matcher(eanCode.trim()).matches();
    }

    /**
     * Controleer of XML content binnen de toegestane grootte valt.
     */
    public static boolean isWithinSizeLimit(String content) {
        return content != null && content.length() <= MAX_XML_SIZE;
    }

    /**
     * Verwijder potentieel gevaarlijke control characters uit een string.
     * Behoudt normale whitespace (spatie, tab, newline, carriage return).
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        // Verwijder alle control characters behalve \t, \n, \r
        return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }
}
