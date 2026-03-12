package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.MarketResponse;
import nl.engie.allocation.model.enums.*;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.MarketResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Phase 5 steps: 5A through 5D.
 */
@ExtendWith(MockitoExtension.class)
class Phase5StepTests {

    private MarketMessage createMessage() {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid("test-uuid")
                .xmlContent("<test/>")
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("Step 5A: Versturen ACK/NACK")
    class Step5aTests {

        @Mock
        private MarketResponseRepository responseRepository;
        private Step5aVersturenAckNack step;

        @BeforeEach
        void setUp() {
            step = new Step5aVersturenAckNack(responseRepository);
        }

        @Test
        void stepCode_shouldBeStep5A() {
            assertEquals(StepCode.STEP_5A, step.getStepCode());
        }

        @Test
        void execute_withResponse_shouldSendAndUpdateStatus() {
            MarketResponse resp = new MarketResponse();
            resp.setResponseType(ResponseType.ACK);
            resp.setResponseUuid("resp-uuid");
            when(responseRepository.findByMarketMessageId(1L)).thenReturn(Optional.of(resp));

            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals(MessageStatus.RESPONSE_SENT, ctx.getMessage().getStatus());
            assertNotNull(resp.getSentAt());
            verify(responseRepository).save(resp);
        }

        @Test
        void execute_withoutResponse_shouldFail() {
            when(responseRepository.findByMarketMessageId(1L)).thenReturn(Optional.empty());

            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Step 5B: Geconfigureerd versturen")
    class Step5bTests {

        private Step5bGeconfigureerdVersturen step;

        @BeforeEach
        void setUp() {
            step = new Step5bGeconfigureerdVersturen();
        }

        @Test
        void stepCode_shouldBeStep5B() {
            assertEquals(StepCode.STEP_5B, step.getStepCode());
        }

        @Test
        void execute_ackMessage_shouldSucceed() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(false);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
        }

        @Test
        void execute_nackWithoutForward_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            ctx.setAttribute("forwardNackInternally", false);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
        }

        @Test
        void execute_nackWithForward_shouldSucceed() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            ctx.setAttribute("forwardNackInternally", true);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
        }
    }

    @Nested
    @DisplayName("Step 5C: Logging verzendtijd")
    class Step5cTests {

        private Step5cLoggingVerzendtijd step;

        @BeforeEach
        void setUp() {
            step = new Step5cLoggingVerzendtijd();
        }

        @Test
        void stepCode_shouldBeStep5C() {
            assertEquals(StepCode.STEP_5C, step.getStepCode());
        }

        @Test
        void execute_shouldLogSendTime() {
            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertNotNull(ctx.getAttribute("responseSentAt", LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Step 5D: Zelfstandig versturen")
    class Step5dTests {

        private Step5dZelfstandigVersturen step;

        @BeforeEach
        void setUp() {
            step = new Step5dZelfstandigVersturen();
        }

        @Test
        void stepCode_shouldBeStep5D() {
            assertEquals(StepCode.STEP_5D, step.getStepCode());
        }

        @Test
        void execute_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
        }
    }
}
