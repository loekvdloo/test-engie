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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

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
                String validAllocationXml1 = withDateWindow(VALID_ALLOCATION_XML_1, -12, 12);
                String validAllocationXml2 = withDateWindow(VALID_ALLOCATION_XML_2, -18, 6);
                String validAllocationXml3 = withDateWindow(VALID_ALLOCATION_XML_3, -10, 14);
                String validAggregatedXml = withDateWindow(VALID_AGGREGATED_XML, -20, 4);
                String validRcfXml = withDateWindow(VALID_RCF_XML, -16, 8);
                String validNegativeVolumePrfXml = withDateWindow(VALID_NEGATIVE_VOLUME_PRF_XML, -22, 2);
                String valid96PositionsXml = withDateWindow(VALID_96_POSITIONS_XML, -8, 16);
                String validMultiSeriesXml = withDateWindow(VALID_MULTI_SERIES_XML, -6, 18);
                String validAllocationXmlDdm = withDateWindow(VALID_ALLOCATION_XML_DDM, -14, 10);

        // =====================================================
        // GELDIGE BERICHTEN (verwacht: ACK)
        // =====================================================

        // === 0. Drie expliciet geldige berichten met unieke IDs (garandeert ACK-voorbeelden) ===
        messageService.submitMessage(new MessageSubmitRequest(
                buildValidAllocationAckXml(UUID.randomUUID().toString()), false, "871686700000000001"));
        count++;
        messageService.submitMessage(new MessageSubmitRequest(
                buildValidAggregatedAckXml(UUID.randomUUID().toString()), false, "871686700000000002"));
        count++;
        messageService.submitMessage(new MessageSubmitRequest(
                buildValidFactorAckXml(UUID.randomUUID().toString()), false, "871686700000000003"));
        count++;

        // === 1. Geldig allocatiebericht - Engie (DDK) ===
        messageService.submitMessage(new MessageSubmitRequest(
                validAllocationXml1, false, "871686700000000001"));
        count++;

        // === 2. Geldig allocatiebericht - Vattenfall (DDK) ===
        messageService.submitMessage(new MessageSubmitRequest(
                validAllocationXml2, false, "871686700000000002"));
        count++;

        // === 3. Geldig bericht met handmatige opvoer - Essent (DDQ) ===
        messageService.submitMessage(new MessageSubmitRequest(
                validAllocationXml3, true, "871686700000000003"));
        count++;

        // === 4. Geaggregeerd allocatiebericht met PRF groep ===
        messageService.submitMessage(new MessageSubmitRequest(
                validAggregatedXml, false, "871686700000000001"));
        count++;

        // === 5. RCF / Profielfracties bericht ===
        messageService.submitMessage(new MessageSubmitRequest(
                validRcfXml, false, "871686700000000002"));
        count++;

        // === 6. Negatief volume (PRF groep = toegestaan) ===
        messageService.submitMessage(new MessageSubmitRequest(
                validNegativeVolumePrfXml, false, "871686700000000001"));
        count++;

        // === 7. Groot bericht met 96 posities (volledige dag bij PT15M) ===
        messageService.submitMessage(new MessageSubmitRequest(
                valid96PositionsXml, false, "871686700000000001"));
        count++;

        // === 8. Meerdere tijdseries in één bericht ===
        messageService.submitMessage(new MessageSubmitRequest(
                validMultiSeriesXml, false, "871686700000000003"));
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
                validAllocationXml1, false, null));
        count++;

        // === 11. Ongeldige XML - niet parseerbaar (technische fout 1C) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_MALFORMED_XML, false, "871686700000000001"));
        count++;

        // === 12. Onbekende BRP EAN (NACK - foutcode 765) ===
        messageService.submitMessage(new MessageSubmitRequest(
                validAllocationXml1, false, "999999999999999999"));
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
                validAllocationXmlDdm, false, "871686700000000004"));
        count++;

        // =====================================================
        // FOUTCODES 651 t/m 782 — elk een eigen testgeval
        // =====================================================

        // === 21. Ongeldige EAN-18 zendercode (NACK - foutcode 651) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_EAN18_SENDER_XML, false, null));
        count++;

        // === 22. Eerder ontvangen bericht met zelfde kenmerk (NACK - foutcode 670) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_DUPLICATE_KENMERK_XML, false, "871686700000000001"));
        count++;

        // === 23. Onjuist aantal posities t.o.v. verwacht (NACK - foutcode 671) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_POSITION_COUNT_XML, false, "871686700000000001"));
        count++;

        // === 24. ProcessTypeID past niet bij berichtinhoud (NACK - foutcode 681) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_PROCESS_TYPE_XML, false, "871686700000000001"));
        count++;

        // === 25. SenderID in bericht ≠ geregistreerde afzender (NACK - foutcode 701) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_SENDER_MISMATCH_XML, false, "871686700000000002"));
        count++;

        // === 26. Recentere creatiedatum al ontvangen (NACK - foutcode 704) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_NOT_LATEST_VERSION_XML, false, "871686700000000001"));
        count++;

        // === 27. ReceiverID past niet bij ontvanger (NACK - foutcode 745) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_RECEIVER_MISMATCH_XML, false, "871686700000000001"));
        count++;

        // === 28. ProcessTypeID past niet bij ontvanger rol (NACK - foutcode 747) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_RECEIVER_ROL_XML, false, "871686700000000001"));
        count++;

        // === 29. ContentType niet in lijn met ProcessTypeID (NACK - foutcode 754) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_CONTENT_TYPE_XML, false, "871686700000000001"));
        count++;

        // === 30. Ontbrekende EAN-13 / mRID in bericht (NACK - foutcode 758) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_NO_MRID_XML, false, "871686700000000001"));
        count++;

        // === 31. BRP niet actief als BRP-er in het netgebied (NACK - foutcode 761) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_BRP_NOT_ACTIVE_XML, false, "871686700000000001"));
        count++;

        // === 32. Aantal tijdseries past niet bij allocatiegroep (NACK - foutcode 764) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_AGGREGATED_NO_GROUP_XML, false, "871686700000000001"));
        count++;

        // === 33. Duplicate allocatierun identificatie (NACK - foutcode 769) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_DUPLICATE_ALLOCATIERUN_XML, false, "871686700000000001"));
        count++;

        // === 34. Vastgesteld afnametype past niet bij profielcategorie (NACK - foutcode 771) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_AFNAMETYPE_XML, false, "871686700000000001"));
        count++;

        // === 35. Factor heeft onjuist aantal decimalen (NACK - foutcode 774) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_FACTOR_DECIMALS_XML, false, "871686700000000001"));
        count++;

        // === 36. Volume heeft onjuist aantal decimalen (NACK - foutcode 776) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_VOLUME_DECIMALS_XML, false, "871686700000000001"));
        count++;

        // === 37. Netgebied niet in LNB administratie (NACK - foutcode 777) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_NETGEBIED_UNKNOWN_XML, false, "871686700000000001"));
        count++;

        // === 38. Aantal profielfractie tijdseries onjuist (NACK - foutcode 779) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_PROFIELFRACTIE_COUNT_XML, false, "871686700000000001"));
        count++;

        // === 39. CorrelationID mismatch in header (NACK - foutcode 780) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_CORRELATION_ID_XML, false, "871686700000000001"));
        count++;

        // === 40. Status profielfracties past niet bij profielcategorie (NACK - foutcode 781) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_STATUS_PROFIELFRACTIES_XML, false, "871686700000000001"));
        count++;

        // === 41. Gat in positienummering (NACK - foutcode 782) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_POSITION_GAP_XML, false, "871686700000000001"));
        count++;

        // === 42. Aansluit-EAN18 heeft onjuiste checkdigit (NACK - foutcode 650) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_AANSLUITING_EAN18_CHECKDIGIT_XML, false, "871686700000000001"));
        count++;

        // === 43. Ongeldige energie-eenheid (NACK - foutcode 668) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_ENERGY_UNIT_XML, false, "871686700000000001"));
        count++;

        // === 44. Ongeldige herkomst/validatie/reparatiemethodiek-combinatie (NACK - foutcode 683) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_ORIGIN_VALIDATION_REPAIR_XML, false, "871686700000000001"));
        count++;

        // === 45. Aggregated bericht met onjuiste EAN-13 telling (NACK - foutcode 758) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_EAN13_COUNT_AGG_XML, false, "871686700000000001"));
        count++;

        // === 46. AllocationSeries zonder BRP EAN-13 (NACK - foutcode 759) ===
        messageService.submitMessage(new MessageSubmitRequest(
                INVALID_MISSING_BRP_EAN13_XML, false, "871686700000000001"));
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

    /** Foutieve resolutie PT1H i.p.v. PT15M bij AllocationFactorSeries (triggers foutcode 773) */
    private static final String INVALID_RESOLUTION_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationFactorSeriesNotification>
                <mRID>e5f6a7b8-c9d0-1234-efab-345678901234</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT1H</resolution>
                <position>1</position><quantity>500.000</quantity>
            </AllocationFactorSeriesNotification>
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

    // ============================================================
    // Test XML Templates — één per foutcode (651 t/m 782)
    // ============================================================

    /** Foutcode 651: EAN-18 zendercode heeft niet precies 18 cijfers */
    private static final String INVALID_EAN18_SENDER_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>65100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID>12345678</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-01T00:00:00Z</startDateTime>
                <endDateTime>2025-07-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 670: eerder ontvangen bericht met zelfde kenmerk (duplicaat-indicator in XML) */
    private static final String INVALID_DUPLICATE_KENMERK_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>67000000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-03T00:00:00Z</startDateTime>
                <endDateTime>2025-07-04T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <duplicaatKenmerk>JA</duplicaatKenmerk>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 671: opgegeven aantal posities klopt niet met werkelijke inhoud */
    private static final String INVALID_POSITION_COUNT_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>67100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-05T00:00:00Z</startDateTime>
                <endDateTime>2025-07-06T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <positionCount>96</positionCount>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
                <position>3</position><quantity>150.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 681: ProcessTypeID past niet bij berichtinhoud */
    private static final String INVALID_PROCESS_TYPE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>68100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <processTypeID>Z99</processTypeID>
                <startDateTime>2025-07-07T00:00:00Z</startDateTime>
                <endDateTime>2025-07-08T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 701: SenderID in XML ≠ geregistreerde EAN van afzender */
    private static final String INVALID_SENDER_MISMATCH_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>70100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID>871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-09T00:00:00Z</startDateTime>
                <endDateTime>2025-07-10T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 704: recentere creatiedatum al eerder ontvangen */
    private static final String INVALID_NOT_LATEST_VERSION_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>70400000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <isLatestVersion>NEEN</isLatestVersion>
                <startDateTime>2025-07-11T00:00:00Z</startDateTime>
                <endDateTime>2025-07-12T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 745: ReceiverID in XML past niet bij verwachte ontvanger */
    private static final String INVALID_RECEIVER_MISMATCH_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>74500000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID>999999999999999999</receiver_MarketParticipant.mRID>
                                <soapReceiverID>8716867000013</soapReceiverID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-13T00:00:00Z</startDateTime>
                <endDateTime>2025-07-14T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 747: ProcessTypeID past niet bij ontvanger rol */
    private static final String INVALID_RECEIVER_ROL_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>74700000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                                <receiverRole>LNB</receiverRole>
                                <processTypeID>N131</processTypeID>
                <startDateTime>2025-07-15T00:00:00Z</startDateTime>
                <endDateTime>2025-07-16T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 754: ContentType in SOAP Header niet in lijn met ProcessTypeID */
    private static final String INVALID_CONTENT_TYPE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>75400000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                                <processTypeID>N151</processTypeID>
                                <contentType>AllocationSeries</contentType>
                <startDateTime>2025-07-17T00:00:00Z</startDateTime>
                <endDateTime>2025-07-18T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 758: ontbrekende EAN-13 / mRID identificatie in bericht */
    private static final String INVALID_NO_MRID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-19T00:00:00Z</startDateTime>
                <endDateTime>2025-07-20T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 761: BRP niet actief als BRP-er in het netgebied */
    private static final String INVALID_BRP_NOT_ACTIVE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>76100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <brpActief>NEE</brpActief>
                <startDateTime>2025-07-21T00:00:00Z</startDateTime>
                <endDateTime>2025-07-22T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 764: aantal tijdseries in bericht past niet bij allocatiegroep */
    private static final String INVALID_AGGREGATED_NO_GROUP_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AggregatedAllocationSeriesNotification>
                <mRID>76400000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-23T00:00:00Z</startDateTime>
                <endDateTime>2025-07-24T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AggregatedAllocationSeriesNotification>
            """;

    /** Foutcode 769: allocatierun identificatie reeds eerder ontvangen */
    private static final String INVALID_DUPLICATE_ALLOCATIERUN_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>76900000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <allocatieRunId>REEDS-VERWERKT-20250601-001</allocatieRunId>
                <startDateTime>2025-07-25T00:00:00Z</startDateTime>
                <endDateTime>2025-07-26T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 771: vastgesteld afnametype past niet bij profielcategorie */
    private static final String INVALID_AFNAMETYPE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>77100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <vastgesteldAfnametype>MISMATCH</vastgesteldAfnametype>
                <startDateTime>2025-07-27T00:00:00Z</startDateTime>
                <endDateTime>2025-07-28T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 774: factor heeft onjuist aantal decimalen (verwacht 3, gevonden 2) */
    private static final String INVALID_FACTOR_DECIMALS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationFactorSeriesNotification>
                <mRID>77400000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-07-29T00:00:00Z</startDateTime>
                <endDateTime>2025-07-30T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <factor>0.45</factor>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationFactorSeriesNotification>
            """;

    /** Foutcode 776: volume heeft onjuist aantal decimalen (2 i.p.v. 3) */
    private static final String INVALID_VOLUME_DECIMALS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>77600000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-08-01T00:00:00Z</startDateTime>
                <endDateTime>2025-08-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>150.50</quantity>
                <position>2</position><quantity>200.75</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 777: netgebied of allocatiepunt niet in LNB administratie */
    private static final String INVALID_NETGEBIED_UNKNOWN_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>77700000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <netgebiedEAN>500000000000000001</netgebiedEAN>
                <startDateTime>2025-08-03T00:00:00Z</startDateTime>
                <endDateTime>2025-08-04T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 779: aantal tijdseries met profielfracties past niet bij profielcategorie */
    private static final String INVALID_PROFIELFRACTIE_COUNT_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationFactorSeriesNotification>
                <mRID>77900000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <profielfractieCount>0</profielfractieCount>
                <startDateTime>2025-08-05T00:00:00Z</startDateTime>
                <endDateTime>2025-08-06T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationFactorSeriesNotification>
            """;

    /** Foutcode 780: CorrelationID in Business Document Header ≠ CorrelationID in SOAP Header */
    private static final String INVALID_CORRELATION_ID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>78000000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <correlationID>MISMATCH</correlationID>
                                <soapCorrelationID>SOAP-CORR-OTHER</soapCorrelationID>
                <startDateTime>2025-08-07T00:00:00Z</startDateTime>
                <endDateTime>2025-08-08T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    /** Foutcode 781: status profielfracties past niet bij profielcategorie */
    private static final String INVALID_STATUS_PROFIELFRACTIES_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationFactorSeriesNotification>
                <mRID>78100000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <statusProfielfracties>ONGELDIG</statusProfielfracties>
                <startDateTime>2025-08-09T00:00:00Z</startDateTime>
                <endDateTime>2025-08-10T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationFactorSeriesNotification>
            """;

    /** Foutcode 782: gat in positienummering (1, 2, 5 — positie 3 en 4 ontbreken) */
    private static final String INVALID_POSITION_GAP_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>78200000-0000-0000-0000-000000000001</mRID>
                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                <product><identification>023</identification></product>
                <startDateTime>2025-08-11T00:00:00Z</startDateTime>
                <endDateTime>2025-08-12T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
                <position>5</position><quantity>150.000</quantity>
            </AllocationSeries>
            """;

        /** Foutcode 650: aansluiting EAN-18 heeft onjuiste checkdigit */
        private static final String INVALID_AANSLUITING_EAN18_CHECKDIGIT_XML = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <AllocationSeries>
                                <mRID>65000000-0000-0000-0000-000000000001</mRID>
                                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                                <pointmRID>871686700000000002</pointmRID>
                                <product><identification>023</identification></product>
                                <startDateTime>2025-08-13T00:00:00Z</startDateTime>
                                <endDateTime>2025-08-14T00:00:00Z</endDateTime>
                                <resolution>PT15M</resolution>
                                <position>1</position><quantity>100.000</quantity>
                                <position>2</position><quantity>200.000</quantity>
                        </AllocationSeries>
                        """;

        /** Foutcode 668: energie-eenheid past niet bij product */
        private static final String INVALID_ENERGY_UNIT_XML = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <AllocationSeries>
                                <mRID>66800000-0000-0000-0000-000000000001</mRID>
                                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                                <product><identification>023</identification></product>
                                <energyUnit>MWH</energyUnit>
                                <startDateTime>2025-08-15T00:00:00Z</startDateTime>
                                <endDateTime>2025-08-16T00:00:00Z</endDateTime>
                                <resolution>PT15M</resolution>
                                <position>1</position><quantity>100.000</quantity>
                                <position>2</position><quantity>200.000</quantity>
                        </AllocationSeries>
                        """;

        /** Foutcode 683: ongeldige combinatie herkomstindicatie/validatiestatus/reparatiemethodiek */
        private static final String INVALID_ORIGIN_VALIDATION_REPAIR_XML = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <AllocationSeries>
                                <mRID>68300000-0000-0000-0000-000000000001</mRID>
                                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                                <receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID>
                                <product><identification>023</identification></product>
                                <originIndicator>ONGELDIG</originIndicator>
                                <validationStatus>ONGELDIG</validationStatus>
                                <repairMethod>ONGELDIG</repairMethod>
                                <startDateTime>2025-08-17T00:00:00Z</startDateTime>
                                <endDateTime>2025-08-18T00:00:00Z</endDateTime>
                                <resolution>PT15M</resolution>
                                <position>1</position><quantity>100.000</quantity>
                                <position>2</position><quantity>200.000</quantity>
                        </AllocationSeries>
                        """;

        /** Foutcode 758: geaggregeerd bericht met onjuiste EAN-13 telling */
        private static final String INVALID_EAN13_COUNT_AGG_XML = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <AggregatedAllocationSeriesNotification>
                                <mRID>75800000-0000-0000-0000-000000000001</mRID>
                                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                                <receiver_MarketParticipant.mRID codingScheme="A10">871686700000000002</receiver_MarketParticipant.mRID>
                                <product><identification>023</identification></product>
                                <group_businessType>TMT</group_businessType>
                                <startDateTime>2025-08-19T00:00:00Z</startDateTime>
                                <endDateTime>2025-08-20T00:00:00Z</endDateTime>
                                <resolution>PT15M</resolution>
                                <position>1</position><quantity>100.000</quantity>
                                <position>2</position><quantity>200.000</quantity>
                        </AggregatedAllocationSeriesNotification>
                        """;

        /** Foutcode 759: individueel bericht zonder BRP EAN-13 */
        private static final String INVALID_MISSING_BRP_EAN13_XML = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <AllocationSeries>
                                <mRID>75900000-0000-0000-0000-000000000001</mRID>
                                <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                                <receiver_MarketParticipant.mRID codingScheme="A10">871686700000000002</receiver_MarketParticipant.mRID>
                                <product><identification>023</identification></product>
                                <marketRole>BRP</marketRole>
                                <startDateTime>2025-08-21T00:00:00Z</startDateTime>
                                <endDateTime>2025-08-22T00:00:00Z</endDateTime>
                                <resolution>PT15M</resolution>
                                <position>1</position><quantity>100.000</quantity>
                                <position>2</position><quantity>200.000</quantity>
                        </AllocationSeries>
                        """;

        private String withDateWindow(String xml, int startOffsetHours, int endOffsetHours) {
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                String start = now.plusHours(startOffsetHours).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                String end = now.plusHours(endOffsetHours).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";

                return xml
                                .replaceAll("<startDateTime>[^<]+</startDateTime>", "<startDateTime>" + start + "</startDateTime>")
                                .replaceAll("<endDateTime>[^<]+</endDateTime>", "<endDateTime>" + end + "</endDateTime>");
        }

        private String buildValidAllocationAckXml(String messageId) {
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                String start = now.minusHours(12).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                String end = now.plusHours(12).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                return """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <AllocationSeries>
                                        <mRID>%s</mRID>
                                        <sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID>
                                        <receiver_MarketParticipant.mRID codingScheme="A10">871686700000000010</receiver_MarketParticipant.mRID>
                                        <suppliermRID>8716867000013</suppliermRID>
                                        <product><identification>023</identification></product>
                                        <startDateTime>%s</startDateTime>
                                        <endDateTime>%s</endDateTime>
                                        <resolution>PT15M</resolution>
                                        <position>1</position><quantity>100.000</quantity>
                                        <position>2</position><quantity>200.000</quantity>
                                </AllocationSeries>
                                """.formatted(messageId, start, end);
        }

        private String buildValidAggregatedAckXml(String messageId) {
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                String start = now.minusHours(12).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                String end = now.plusHours(12).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                return """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <AggregatedAllocationSeriesNotification>
                                        <mRID>%s</mRID>
                                        <sender_MarketParticipant.mRID codingScheme="A10">871686700000000002</sender_MarketParticipant.mRID>
                                        <receiver_MarketParticipant.mRID codingScheme="A10">871686700000000011</receiver_MarketParticipant.mRID>
                                        <suppliermRID>8716867000013</suppliermRID>
                                        <product><identification>023</identification></product>
                                        <group_businessType>TMT</group_businessType>
                                        <startDateTime>%s</startDateTime>
                                        <endDateTime>%s</endDateTime>
                                        <resolution>PT15M</resolution>
                                        <position>1</position><quantity>100.000</quantity>
                                        <position>2</position><quantity>200.000</quantity>
                                </AggregatedAllocationSeriesNotification>
                                """.formatted(messageId, start, end);
        }

        private String buildValidFactorAckXml(String messageId) {
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                String start = now.minusHours(12).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                String end = now.plusHours(12).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                return """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <AllocationFactorSeriesNotification>
                                        <mRID>%s</mRID>
                                        <sender_MarketParticipant.mRID codingScheme="A10">871686700000000003</sender_MarketParticipant.mRID>
                                        <receiver_MarketParticipant.mRID codingScheme="A10">871686700000000012</receiver_MarketParticipant.mRID>
                                        <suppliermRID>8716867000013</suppliermRID>
                                        <processTypeID>N151</processTypeID>
                                        <contentType>AllocationFactorSeries</contentType>
                                        <product><identification>023</identification></product>
                                        <dateRCF_version>2026-03-16</dateRCF_version>
                                        <startDateTime>%s</startDateTime>
                                        <endDateTime>%s</endDateTime>
                                        <resolution>PT15M</resolution>
                                        <position>1</position><quantity>0.450</quantity>
                                        <position>2</position><quantity>0.520</quantity>
                                </AllocationFactorSeriesNotification>
                                """.formatted(messageId, start, end);
        }
}
