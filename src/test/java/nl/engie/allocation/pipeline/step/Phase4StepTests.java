package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.MarketResponse;
import nl.engie.allocation.model.entity.ValidationResult;
import nl.engie.allocation.model.enums.*;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.MarketResponseRepository;
import nl.engie.allocation.repository.ValidationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Phase 4 steps: 4A through 4E.
 */
@ExtendWith(MockitoExtension.class)
class Phase4StepTests {

    private static final String VALID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries><mRID>test</mRID></AllocationSeries>
            """;

    private MarketMessage createMessage() {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid("test-uuid")
                .xmlContent(VALID_XML)
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .messageType(MessageType.ALLOCATION_SERIES)
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("Step 4A: Genereer ACK")
    class Step4aTests {

        @Mock
        private MarketResponseRepository responseRepository;
        private Step4aGenereerAck step;

        @BeforeEach
        void setUp() {
            step = new Step4aGenereerAck(responseRepository);
        }

        @Test
        void stepCode_shouldBeStep4A() {
            assertEquals(StepCode.STEP_4A, step.getStepCode());
        }

        @Test
        void execute_noErrors_shouldGenerateAck() {
            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
            assertFalse(ctx.isNack());
            assertNotNull(ctx.getResponseXml());
            assertEquals(MessageStatus.ACK_GENERATED, ctx.getMessage().getStatus());

            ArgumentCaptor<MarketResponse> captor = ArgumentCaptor.forClass(MarketResponse.class);
            verify(responseRepository).save(captor.capture());
            assertEquals(ResponseType.ACK, captor.getValue().getResponseType());
        }

        @Test
        void execute_withErrors_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.addValidationError("TEST", "Test error");

            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
            verify(responseRepository, never()).save(any());
        }

        @Test
        void execute_withBusinessInvalid_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setBusinessValid(false);

            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
        }
    }

    @Nested
    @DisplayName("Step 4B: Genereer NACK")
    class Step4bTests {

        @Mock
        private MarketResponseRepository responseRepository;
        private Step4bGenereerNack step;

        @BeforeEach
        void setUp() {
            step = new Step4bGenereerNack(responseRepository);
        }

        @Test
        void stepCode_shouldBeStep4B() {
            assertEquals(StepCode.STEP_4B, step.getStepCode());
        }

        @Test
        void execute_withErrors_shouldGenerateNack() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.addValidationError("BRP001", "EAN niet gevonden");

            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
            assertTrue(ctx.isNack());
            assertEquals(MessageStatus.NACK_GENERATED, ctx.getMessage().getStatus());

            ArgumentCaptor<MarketResponse> captor = ArgumentCaptor.forClass(MarketResponse.class);
            verify(responseRepository).save(captor.capture());
            assertEquals(ResponseType.NACK, captor.getValue().getResponseType());
            assertTrue(captor.getValue().getErrorCodes().contains("BRP001"));
        }

        @Test
        void execute_noErrors_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
            verify(responseRepository, never()).save(any());
        }

        @Test
        void execute_withPipelineErrors_shouldGenerateNack() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.addError("Pipeline error occurred");

            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertTrue(ctx.isNack());
        }
    }

    @Nested
    @DisplayName("Step 4C: Foutcodes")
    class Step4cTests {

        private Step4cFoutcodes step;

        @BeforeEach
        void setUp() {
            step = new Step4cFoutcodes();
        }

        @Test
        void stepCode_shouldBeStep4C() {
            assertEquals(StepCode.STEP_4C, step.getStepCode());
        }

        @Test
        void execute_withNack_shouldSucceed() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            ctx.addValidationError("ERR1", "Error 1");
            ctx.addValidationError("ERR2", "Error 2");

            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("2 codes"));
        }

        @Test
        void execute_withoutNack_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
        }
    }

    @Nested
    @DisplayName("Step 4D: Vastleggen resultaat")
    class Step4dTests {

        @Mock
        private ValidationResultRepository validationResultRepository;
        private Step4dVastleggenResultaat step;

        @BeforeEach
        void setUp() {
            step = new Step4dVastleggenResultaat(validationResultRepository);
        }

        @Test
        void stepCode_shouldBeStep4D() {
            assertEquals(StepCode.STEP_4D, step.getStepCode());
        }

        @Test
        void execute_withAck_shouldSaveValidResult() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(false);
            step.execute(ctx);

            ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
            verify(validationResultRepository).save(captor.capture());
            assertTrue(captor.getValue().getIsValid());
            assertEquals("OVERALL", captor.getValue().getRuleCode());
        }

        @Test
        void execute_withNack_shouldSaveInvalidResult() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            step.execute(ctx);

            ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
            verify(validationResultRepository).save(captor.capture());
            assertFalse(captor.getValue().getIsValid());
            assertEquals("VALIDATION_FAILED", captor.getValue().getErrorCode());
        }
    }

    @Nested
    @DisplayName("Step 4E: NACK configuratie")
    class Step4eTests {

        private Step4eNackConfiguratie step;

        @BeforeEach
        void setUp() {
            step = new Step4eNackConfiguratie();
            // forwardNackInternally defaults to false
        }

        @Test
        void stepCode_shouldBeStep4E() {
            assertEquals(StepCode.STEP_4E, step.getStepCode());
        }

        @Test
        void execute_withoutNack_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
        }

        @Test
        void execute_withNack_shouldSetForwardAttribute() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertNotNull(ctx.getAttribute("forwardNackInternally", Boolean.class));
        }
    }
}
