package nl.engie.allocation.model.enums;

/**
 * Types of market messages based on the allocation data specification (test1.docx).
 */
public enum MessageType {
    ALLOCATION_SERIES("AllocationSeriesNotification", "Allocatiegegevens individueel allocatiepunt"),
    AGGREGATED_ALLOCATION_SERIES("AggregatedAllocationSeriesNotification", "Geaggregeerde allocatiegegevens"),
    ALLOCATION_FACTOR_SERIES("AllocationFactorSeriesNotification", "RCF en Profielfracties"),
    MANUAL_ENTRY("ManualEntry", "Handmatig opgevoerd bericht");

    private final String xmlRootElement;
    private final String description;

    MessageType(String xmlRootElement, String description) {
        this.xmlRootElement = xmlRootElement;
        this.description = description;
    }

    public String getXmlRootElement() { return xmlRootElement; }
    public String getDescription() { return description; }

    public static MessageType fromXmlRoot(String rootElement) {
        for (MessageType type : values()) {
            if (type.xmlRootElement.equalsIgnoreCase(rootElement)) {
                return type;
            }
        }
        return null;
    }
}
