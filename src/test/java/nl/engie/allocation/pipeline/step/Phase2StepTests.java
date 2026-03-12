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
 * Unit tests for Phase 2 steps: 2A through 2E.
 */
class Phase2StepTests {

    private static final String VALID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>a1b2c3d4-e5f6-7890-abcd-ef1234567890</mRID>
            </AllocationSeries>
            """;

    private MarketMessage createMessage(MessageType type) {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid("test-uuid")
                .xmlContent(VALID_XML)
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .messageType(type)
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("Step 2A: Classificeer berichttype")
    class Step2aTests {

        private Step2aClassificeer step;

        @BeforeEach
        void setUp() {
            step = new Step2aClassificeer();
        }

        @Test
        void stepCode_shouldBeStep2A() {
            assertEquals(StepCode.STEP_2A, step.getStepCode());
        }

        @Test
        void execute_allocationSeries_shouldClassifyAsIndividueel() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            ctx.setDetectedMessageType(MessageType.ALLOCATION_SERIES);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals("INDIVIDUEEL_ALLOCATIEPUNT", ctx.getAttribute("classification", String.class));
        }

        @Test
        void execute_aggregatedAllocation_shouldClassifyAsGeaggregeerd() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.AGGREGATED_ALLOCATION_SERIES));
            ctx.setDetectedMessageType(MessageType.AGGREGATED_ALLOCATION_SERIES);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals("GEAGGREGEERD_ALLOCATIE", ctx.getAttribute("classification", String.class));
        }

        @Test
        void execute_allocationFactor_shouldClassifyAsRcf() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_FACTOR_SERIES));
            ctx.setDetectedMessageType(MessageType.ALLOCATION_FACTOR_SERIES);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals("RCF_PROFIELFRACTIES", ctx.getAttribute("classification", String.class));
        }

        @Test
        void execute_noType_shouldFail() {
            MarketMessage msg = createMessage(null);
            PipelineContext ctx = new PipelineContext(msg);
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Step 2B: Bepalen prioriteit")
    class Step2bTests {

        private Step2bBepalenPrioriteit step;

        @BeforeEach
        void setUp() {
            step = new Step2bBepalenPrioriteit();
        }

        @Test
        void stepCode_shouldBeStep2B() {
            assertEquals(StepCode.STEP_2B, step.getStepCode());
        }

        @Test
        void execute_allocationSeries_shouldHaveHighestPriority() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            ctx.setDetectedMessageType(MessageType.ALLOCATION_SERIES);
            step.execute(ctx);

            assertEquals(1, ctx.getAssignedPriority());
            assertEquals(1, ctx.getMessage().getPriority());
        }

        @Test
        void execute_aggregated_shouldHavePriority2() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.AGGREGATED_ALLOCATION_SERIES));
            ctx.setDetectedMessageType(MessageType.AGGREGATED_ALLOCATION_SERIES);
            step.execute(ctx);

            assertEquals(2, ctx.getAssignedPriority());
        }

        @Test
        void execute_manualEntry_shouldHavePriority5() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.MANUAL_ENTRY));
            ctx.setDetectedMessageType(MessageType.MANUAL_ENTRY);
            step.execute(ctx);

            assertEquals(5, ctx.getAssignedPriority());
        }
    }

    @Nested
    @DisplayName("Step 2C: Plaatsen wachtrij")
    class Step2cTests {

        private Step2cPlaatsenWachtrij step;

        @BeforeEach
        void setUp() {
            step = new Step2cPlaatsenWachtrij();
        }

        @Test
        void stepCode_shouldBeStep2C() {
            assertEquals(StepCode.STEP_2C, step.getStepCode());
        }

        @Test
        void execute_shouldSetQueuedAttributes() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals(true, ctx.getAttribute("queued", Boolean.class));
            assertNotNull(ctx.getAttribute("queuedAt", LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Step 2D: Uitzondering parkeren")
    class Step2dTests {

        private Step2dUitzonderingParkeren step;

        @BeforeEach
        void setUp() {
            step = new Step2dUitzonderingParkeren();
        }

        @Test
        void stepCode_shouldBeStep2D() {
            assertEquals(StepCode.STEP_2D, step.getStepCode());
        }

        @Test
        void execute_whenTechnicallyValid_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            ctx.setTechnicallyValid(true);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
            assertFalse(ctx.isParked());
        }

        @Test
        void execute_whenTechnicallyInvalid_shouldPark() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            ctx.setTechnicallyValid(false);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
            assertTrue(ctx.isParked());
            assertEquals(MessageStatus.PARKED, ctx.getMessage().getStatus());
        }
    }

    @Nested
    @DisplayName("Step 2E: Uitval opnieuw verwerken")
    class Step2eTests {

        private Step2eUitvalOpnieuw step;

        @BeforeEach
        void setUp() {
            step = new Step2eUitvalOpnieuw();
        }

        @Test
        void stepCode_shouldBeStep2E() {
            assertEquals(StepCode.STEP_2E, step.getStepCode());
        }

        @Test
        void execute_notRetry_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
        }

        @Test
        void execute_isRetry_shouldIncrementCounter() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            ctx.setAttribute("isRetry", true);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
            assertEquals(1, ctx.getAttribute("retryCount", Integer.class));
        }

        @Test
        void execute_secondRetry_shouldIncrementTo2() {
            PipelineContext ctx = new PipelineContext(createMessage(MessageType.ALLOCATION_SERIES));
            ctx.setAttribute("isRetry", true);
            ctx.setAttribute("retryCount", 1);
            step.execute(ctx);

            assertEquals(2, ctx.getAttribute("retryCount", Integer.class));
        }
    }
}
