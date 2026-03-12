package nl.engie.allocation.controller;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import nl.engie.allocation.dto.MessageSubmitRequest;
import nl.engie.allocation.service.MarketMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller voor het laden van test data (alleen beschikbaar in dev/test omgeving).
 *
 * <p>Beveiligingsmaatregel: deze controller is NIET beschikbaar in productie.
 * Alleen actief als het Spring profiel "default", "dev", "test", of "postgres" actief is.
 * Bij productie-deployment moet een apart profiel (bijv. "prod") gebruikt worden.
 */
@RestController
@RequestMapping("/api/test")
@Profile({"default", "dev", "test", "postgres"})
public class TestDataController {

    private static final Logger log = LoggerFactory.getLogger(TestDataController.class);

    private final MarketMessageService messageService;
    private final EntityManager entityManager;

    public TestDataController(MarketMessageService messageService, EntityManager entityManager) {
        this.messageService = messageService;
        this.entityManager = entityManager;
    }

    /**
     * Wis alle testdata uit de database (TRUNCATE CASCADE).
     */
    @PostMapping("/clear")
    @Transactional
    public ResponseEntity<Map<String, String>> clearTestData() {
        log.info("Clearing all test data...");
        entityManager.createNativeQuery("TRUNCATE TABLE delivery_records, market_responses, processing_logs, processing_steps, validation_results, validation_rules, market_messages RESTART IDENTITY CASCADE").executeUpdate();
        log.info("All test data cleared");
        return ResponseEntity.ok(Map.of("message", "Alle testdata gewist"));
    }

    /**
     * Seed de database met 20 diverse testberichten:
     * - 8 geldige berichten (ACK) met verschillende typen en scenario's
     * - 12 ongeldige berichten (NACK/FAILED) die elk een andere foutcode triggeren
     *
     * Dit geeft een goed overzicht van alle pipeline-paden en validatieregels.
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedTestData() {
        log.info("Seeding test data...");

        int count = 0;

        // =====================================================
        // GELDIGE BERICHTEN (verwacht: ACK)
        // =====================================================

        // === 1. Geldig allocatiebericht - Engie (DDK) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_1, false, "871686700000000001"));
        count++;

        // === 2. Geldig allocatiebericht - Vattenfall (DDK) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_2, false, "871686700000000002"));
        count++;

        // === 3. Geldig bericht met handmatige opvoer - Essent (DDQ) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_3, true, "871686700000000003"));
        count++;

        // === 4. Geaggregeerd allocatiebericht met PRF groep ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_AGGREGATED_XML, false, "871686700000000001"));
        count++;

        // === 5. RCF / Profielfracties bericht ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_RCF_XML, false, "871686700000000002"));
        count++;

        // === 6. Negatief volume (PRF groep = toegestaan) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_NEGATIVE_VOLUME_PRF_XML, false, "871686700000000001"));
        count++;

        // === 7. Groot bericht met 96 posities (volledige dag bij PT15M) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_96_POSITIONS_XML, false, "871686700000000001"));
        count++;

        // === 8. Meerdere tijdseries in één bericht ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_MULTI_SERIES_XML, false, "871686700000000003"));
        count++;

        // =====================================================
        // ONGELDIGE BERICHTEN (verwacht: NACK of FAILED)
        // =====================================================

        // === 9. Ongeldig productcode (NACK - validatiefout 3D) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_NO_PRODUCT_XML, false, "871686700000000001"));
        count++;

        // === 10. Geen EAN-code (NACK - BRP onbekend 3A) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_1, false, null));
        count++;

        // === 11. Ongeldige XML - niet parseerbaar (technische fout 1C) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_MALFORMED_XML, false, "871686700000000001"));
        count++;

        // === 12. Onbekende BRP EAN (NACK - foutcode 765) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_1, false, "999999999999999999"));
        count++;

        // === 13. Foutieve resolutie PT1H (NACK - foutcode 773) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_RESOLUTION_XML, false, "871686700000000001"));
        count++;

        // === 14. Periode fout: einddatum vóór startdatum (NACK - foutcode 663) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_PERIOD_XML, false, "871686700000000001"));
        count++;

        // === 15. Ongeldige UUID in mRID (NACK - foutcode 669) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_MRID_XML, false, "871686700000000001"));
        count++;

        // === 16. Negatief volume, niet-PRF (NACK - foutcode 686) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_NEGATIVE_VOLUME_XML, false, "871686700000000001"));
        count++;

        // === 17. Volgorde posities fout: begint bij 2 (NACK - foutcode 676/782) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_POSITION_ORDER_XML, false, "871686700000000001"));
        count++;

        // === 18. Lege XML content (FAILED - stap 1A) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_EMPTY_XML, false, "871686700000000001"));
        count++;

        // === 19. Toekomstdata ver in de toekomst (NACK - foutcode 772) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_FUTURE_DATE_XML, false, "871686700000000001"));
        count++;

        // === 20. Geldig bericht met DDM rol (Liander) ===
        messageService.submitMessage(new MessageSubmitRequest(
                VALID_ALLOCATION_XML_DDM, false, "871686700000000004"));
        count++;

        log.info("Test data seeded: {} messages created", count);

        return ResponseEntity.ok(Map.of(
                "message", count + " testberichten aangemaakt",
                "count", count));
    }

    // ==============================
    // Test XML Templates - GELDIG
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

    /** Geaggregeerd allocatiebericht met PRF allocatiegroep */
    private static final String VALID_AGGREGATED_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AggregatedAllocationSeriesNotification>
                <mRID>d1e2f3a4-b5c6-7890-1234-abcdef012345</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <group_businessType>PRF</group_businessType>
                <startDateTime>2025-02-01T00:00:00Z</startDateTime>
                <endDateTime>2025-02-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>1200.000</quantity>
                <position>2</position><quantity>1150.000</quantity>
                <position>3</position><quantity>1300.000</quantity>
                <position>4</position><quantity>1250.000</quantity>
            </AggregatedAllocationSeriesNotification>
            """;

    /** RCF / Profielfracties bericht */
    private static final String VALID_RCF_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationFactorSeriesNotification>
                <mRID>e2f3a4b5-c6d7-8901-2345-bcdef0123456</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000002</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <dateRCF_version>2025-02-01</dateRCF_version>
                <startDateTime>2025-02-01T00:00:00Z</startDateTime>
                <endDateTime>2025-02-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>0.450</quantity>
                <position>2</position><quantity>0.520</quantity>
                <position>3</position><quantity>0.380</quantity>
                <position>4</position><quantity>0.410</quantity>
            </AllocationFactorSeriesNotification>
            """;

    /** Negatief volume in PRF groep (toegestaan per specificatie) */
    private static final String VALID_NEGATIVE_VOLUME_PRF_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AggregatedAllocationSeriesNotification>
                <mRID>f3a4b5c6-d7e8-9012-3456-cdef01234567</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <group_businessType>PRF</group_businessType>
                <startDateTime>2025-03-01T00:00:00Z</startDateTime>
                <endDateTime>2025-03-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>-50.000</quantity>
                <position>2</position><quantity>-30.000</quantity>
                <position>3</position><quantity>25.000</quantity>
                <position>4</position><quantity>-15.000</quantity>
            </AggregatedAllocationSeriesNotification>
            """;

    /** Groot bericht met 96 posities (volledige dag bij PT15M = 24h * 4 = 96) */
    private static final String VALID_96_POSITIONS_XML;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>a4b5c6d7-e8f9-0123-4567-def012345678</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-04-01T00:00:00Z</startDateTime>
                <endDateTime>2025-04-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
            """);
        for (int i = 1; i <= 96; i++) {
            double quantity = 100.0 + (i * 2.5);
            sb.append(String.format("    <position>%d</position><quantity>%.3f</quantity>%n", i, quantity));
        }
        sb.append("</AllocationSeries>");
        VALID_96_POSITIONS_XML = sb.toString();
    }

    /** Meerdere detail-series in één bericht */
    private static final String VALID_MULTI_SERIES_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>b5c6d7e8-f9a0-1234-5678-ef0123456789</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000003</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-05-01T00:00:00Z</startDateTime>
                <endDateTime>2025-05-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <Detail_Series>
                    <MarketEvaluationPoint><mRID>871686700000000003</mRID></MarketEvaluationPoint>
                    <position>1</position><quantity>100.000</quantity>
                    <position>2</position><quantity>110.000</quantity>
                </Detail_Series>
                <Detail_Series>
                    <MarketEvaluationPoint><mRID>871686700000000003</mRID></MarketEvaluationPoint>
                    <position>1</position><quantity>200.000</quantity>
                    <position>2</position><quantity>220.000</quantity>
                </Detail_Series>
            </AllocationSeries>
            """;

    /** Geldig bericht voor DDM rol (Liander = netbeheerder) */
    private static final String VALID_ALLOCATION_XML_DDM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>c6d7e8f9-a0b1-2345-6789-f01234567890</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000004</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-06-01T00:00:00Z</startDateTime>
                <endDateTime>2025-06-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>500.000</quantity>
                <position>2</position><quantity>520.000</quantity>
                <position>3</position><quantity>480.000</quantity>
                <position>4</position><quantity>510.000</quantity>
            </AllocationSeries>
            """;

    // ==============================
    // Test XML Templates - ONGELDIG
    // ==============================

    /** Ongeldig productcode (triggers NACK via 3D configureerbare regels) */
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

    /** Ongeldige XML - niet parseerbaar (triggers technische fout in stap 1C) */
    private static final String INVALID_MALFORMED_XML = """
            Dit is geen geldige XML!!!
            <broken><tag>
            """;

    /** Foutieve resolutie PT1H i.p.v. PT15M (triggers foutcode 773) */
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

    /** Periode fout: einddatum vóór startdatum (triggers foutcode 663) */
    private static final String INVALID_PERIOD_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>f6a7b8c9-d0e1-2345-fabc-456789012345</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-06-15T00:00:00Z</startDateTime>
                <endDateTime>2025-06-14T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
            </AllocationSeries>
            """;

    /** Ongeldige UUID in mRID veld (triggers foutcode 669) */
    private static final String INVALID_MRID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>GEEN-GELDIG-UUID-FORMAT</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
            </AllocationSeries>
            """;

    /** Negatief volume zonder PRF groep (triggers foutcode 686) */
    private static final String INVALID_NEGATIVE_VOLUME_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>a7b8c9d0-e1f2-3456-abcd-567890123456</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>-250.000</quantity>
                <position>2</position><quantity>100.000</quantity>
            </AllocationSeries>
            """;

    /** Volgorde posities fout: begint bij 2 (triggers foutcode 676 en/of 782) */
    private static final String INVALID_POSITION_ORDER_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>b8c9d0e1-f2a3-4567-bcde-678901234567</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>2</position><quantity>100.000</quantity>
                <position>4</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Lege XML content (triggers FAILED in stap 1A) */
    private static final String INVALID_EMPTY_XML = "   ";

    /** Toekomstdata ver in de toekomst (triggers foutcode 772) */
    private static final String INVALID_FUTURE_DATE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>c9d0e1f2-a3b4-5678-cdef-789012345678</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2099-01-01T00:00:00Z</startDateTime>
                <endDateTime>2099-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;
}
