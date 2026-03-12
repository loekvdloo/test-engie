package nl.engie.allocation.controller;

import nl.engie.allocation.dto.MessageSubmitRequest;
import nl.engie.allocation.service.MarketMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for seeding test data to demonstrate the pipeline dashboard.
 */
@RestController
@RequestMapping("/api/test")
public class TestDataController {

    private static final Logger log = LoggerFactory.getLogger(TestDataController.class);

    private final MarketMessageService messageService;

    public TestDataController(MarketMessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Seed the database with various test messages:
     * - Valid messages that get ACK
     * - Invalid messages that get NACK with different error codes
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedTestData() {
        log.info("Seeding test data...");

        int count = 0;

        // === 1. VALID message - should get ACK ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_1, false, "871686700000000001"));
        count++;

        // === 2. VALID message - another successful one ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_2, false, "871686700000000002"));
        count++;

        // === 3. VALID message with manual entry ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_3, true, "871686700000000003"));
        count++;

        // === 4. INVALID - missing product code (will fail validation 3D) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_NO_PRODUCT_XML, false, "871686700000000001"));
        count++;

        // === 5. INVALID - no EAN sender (will fail 3B/3C) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_1, false, null));
        count++;

        // === 6. INVALID - malformed XML (will fail technical validation 1C) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_MALFORMED_XML, false, "871686700000000001"));
        count++;

        // === 7. INVALID - unknown BRP EAN (will fail 3A) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_1, false, "999999999999999999"));
        count++;

        // === 8. INVALID - wrong resolution (will fail 3D configurable rules) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_RESOLUTION_XML, false, "871686700000000001"));
        count++;

        log.info("Test data seeded: {} messages created", count);

        return ResponseEntity.ok(Map.of(
                "message", count + " testberichten aangemaakt",
                "count", count));
    }

    // ==============================
    // Test XML Templates
    // ==============================

    private static final String VALID_ALLOCATION_XML_1 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>a1b2c3d4-e5f6-7890-abcd-ef1234567890</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>150.000</quantity>
                <position>2</position><quantity>200.000</quantity>
                <position>3</position><quantity>175.000</quantity>
                <position>4</position><quantity>180.000</quantity>
            </AllocationSeries>
            """;

    private static final String VALID_ALLOCATION_XML_2 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>b2c3d4e5-f6a7-8901-bcde-f12345678901</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000002</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-02T00:00:00Z</startDateTime>
                <endDateTime>2025-01-03T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>300.000</quantity>
                <position>2</position><quantity>310.000</quantity>
                <position>3</position><quantity>290.000</quantity>
                <position>4</position><quantity>305.000</quantity>
            </AllocationSeries>
            """;

    private static final String VALID_ALLOCATION_XML_3 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>c3d4e5f6-a7b8-9012-cdef-123456789012</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000003</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-03T00:00:00Z</startDateTime>
                <endDateTime>2025-01-04T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>250.000</quantity>
                <position>2</position><quantity>260.000</quantity>
            </AllocationSeries>
            """;

    private static final String INVALID_NO_PRODUCT_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>d4e5f6a7-b8c9-0123-defa-234567890123</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>8716867000016</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
            </AllocationSeries>
            """;

    private static final String INVALID_MALFORMED_XML = """
            Dit is geen geldige XML!!!
            <broken><tag>
            """;

    private static final String INVALID_RESOLUTION_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>e5f6a7b8-c9d0-1234-efab-345678901234</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT1H</resolution>
                <position>1</position><quantity>500.000</quantity>
            </AllocationSeries>
            """;
}
