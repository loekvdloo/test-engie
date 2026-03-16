package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.StepResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase 1 steps: 1A through 1F.
 */
class Phase1StepTests {

    private static final String VALID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>a1b2c3d4-e5f6-7890-abcd-ef1234567890</mRID>
                <product><identification>8716867000030</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    private MarketMessage createMessage(String xml) {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid("test-uuid-1234")
                .xmlContent(xml)
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .isManualEntry(false)
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("Step 1A: Ontvangen marktbericht")
    class Step1aTests {

        private Step1aOntvangBericht step;

        @BeforeEach
        void setUp() {
            step = new Step1aOntvangBericht();
        }

        @Test
        void stepCode_shouldBeStep1A() {
            assertEquals(StepCode.STEP_1A, step.getStepCode());
        }

        @Test
        void execute_withValidXml_shouldSucceed() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
        }

        @Test
        void execute_withNullXml_shouldFail() {
            PipelineContext ctx = new PipelineContext(createMessage(null));
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Geen berichtinhoud"));
        }

        @Test
        void execute_withEmptyXml_shouldFail() {
            PipelineContext ctx = new PipelineContext(createMessage(""));
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
        }

        @Test
        void execute_withBlankXml_shouldFail() {
            PipelineContext ctx = new PipelineContext(createMessage("   "));
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Step 1B: Technische ontvangstbevestiging")
    class Step1bTests {

        private Step1bTechnischeOntvangst step;

        @BeforeEach
        void setUp() {
            step = new Step1bTechnischeOntvangst();
        }

        @Test
        void stepCode_shouldBeStep1B() {
            assertEquals(StepCode.STEP_1B, step.getStepCode());
        }

        @Test
        void execute_shouldGenerateReceiptId() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertNotNull(ctx.getAttribute("technicalReceiptId", String.class));
        }

        @Test
        void execute_shouldGenerateUniqueReceiptIds() {
            PipelineContext ctx1 = new PipelineContext(createMessage(VALID_XML));
            PipelineContext ctx2 = new PipelineContext(createMessage(VALID_XML));

            step.execute(ctx1);
            step.execute(ctx2);

            assertNotEquals(
                    ctx1.getAttribute("technicalReceiptId", String.class),
                    ctx2.getAttribute("technicalReceiptId", String.class));
        }
    }

    @Nested
    @DisplayName("Step 1C: Technische validatie")
    class Step1cTests {

        private Step1cTechnischeValidatie step;

        @BeforeEach
        void setUp() {
            step = new Step1cTechnischeValidatie();
        }

        @Test
        void stepCode_shouldBeStep1C() {
            assertEquals(StepCode.STEP_1C, step.getStepCode());
        }

        @Test
        void execute_withValidXml_shouldSucceed() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertTrue(ctx.isTechnicallyValid());
        }

        @Test
        void execute_withInvalidXml_shouldFail() {
            PipelineContext ctx = new PipelineContext(createMessage("<broken><xml"));
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
            assertFalse(ctx.isTechnicallyValid());
        }

        @Test
        void execute_withInvalidXml_shouldAddError() {
            PipelineContext ctx = new PipelineContext(createMessage("<broken><xml"));
            step.execute(ctx);

            assertFalse(ctx.getErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("Step 1D: Logging ontvangsttijd")
    class Step1dTests {

        private Step1dLoggingOntvangsttijd step;

        @BeforeEach
        void setUp() {
            step = new Step1dLoggingOntvangsttijd();
        }

        @Test
        void stepCode_shouldBeStep1D() {
            assertEquals(StepCode.STEP_1D, step.getStepCode());
        }

        @Test
        void execute_shouldAlwaysSucceed() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Step 1E: Identificatie berichttype")
    class Step1eTests {

        private Step1eIdentificatieBerichttype step;

        @BeforeEach
        void setUp() {
            step = new Step1eIdentificatieBerichttype();
        }

        @Test
        void stepCode_shouldBeStep1E() {
            assertEquals(StepCode.STEP_1E, step.getStepCode());
        }

        @Test
        void execute_withAllocationSeries_shouldDetectType() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals(MessageType.ALLOCATION_SERIES, ctx.getDetectedMessageType());
            assertEquals(MessageType.ALLOCATION_SERIES, ctx.getMessage().getMessageType());
        }

        @Test
        void execute_withAggregatedAllocation_shouldDetectType() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <AggregatedAllocation>
                        <mRID>test-uuid</mRID>
                    </AggregatedAllocation>
                    """;
            PipelineContext ctx = new PipelineContext(createMessage(xml));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals(MessageType.AGGREGATED_ALLOCATION_SERIES, ctx.getDetectedMessageType());
        }

        @Test
        void execute_withUnknownRoot_shouldFail() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <OnbekendDocument><data>test</data></OnbekendDocument>
                    """;
            PipelineContext ctx = new PipelineContext(createMessage(xml));
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Onbekend berichttype"));
        }
    }

    @Nested
    @DisplayName("Step 1F: Handmatige opvoer")
    class Step1fTests {

        private Step1fHandmatigeOpvoer step;

        @BeforeEach
        void setUp() {
            step = new Step1fHandmatigeOpvoer();
        }

        @Test
        void stepCode_shouldBeStep1F() {
            assertEquals(StepCode.STEP_1F, step.getStepCode());
        }

        @Test
        void execute_withManualEntry_shouldSucceed() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setIsManualEntry(true);
            PipelineContext ctx = new PipelineContext(msg);

            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
            assertEquals(true, ctx.getAttribute("manualEntry", Boolean.class));
        }

        @Test
        void execute_withoutManualEntry_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertTrue(result.isSkipped());
        }
    }
}
