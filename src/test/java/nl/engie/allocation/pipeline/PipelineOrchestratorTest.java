package nl.engie.allocation.pipeline;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.ProcessingStep;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.model.enums.StepStatus;
import nl.engie.allocation.repository.MarketMessageRepository;
import nl.engie.allocation.repository.ProcessingLogRepository;
import nl.engie.allocation.repository.ProcessingStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineOrchestratorTest {

    @Mock private MarketMessageRepository messageRepository;
    @Mock private ProcessingStepRepository stepRepository;
    @Mock private ProcessingLogRepository logRepository;

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
    @DisplayName("Constructor & Registration")
    class RegistrationTests {

        @Test
        void constructor_shouldRegisterAllStepsInOrder() {
            PipelineStep step1 = mockStep(StepCode.STEP_1A, true);
            PipelineStep step2 = mockStep(StepCode.STEP_2A, true);

            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository,
                    List.of(step2, step1) // intentionally reversed
            );

            assertTrue(orchestrator.getRegisteredSteps().contains(StepCode.STEP_1A));
            assertTrue(orchestrator.getRegisteredSteps().contains(StepCode.STEP_2A));
        }

        @Test
        void constructor_withEmptyList_shouldInitializeWithZeroSteps() {
            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository,
                    List.of()
            );

            assertEquals(0, orchestrator.getRegisteredSteps().size());
            assertFalse(orchestrator.isFullyRegistered());
        }

        @Test
        void isFullyRegistered_withAllSteps_shouldReturnTrue() {
            List<PipelineStep> allSteps = java.util.Arrays.stream(StepCode.values())
                    .map(code -> mockStep(code, true))
                    .toList();

            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository, allSteps
            );

            assertTrue(orchestrator.isFullyRegistered());
        }
    }

    @Nested
    @DisplayName("initializeSteps")
    class InitializeStepsTests {

        private PipelineOrchestrator orchestrator;

        @BeforeEach
        void setUp() {
            orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository, List.of()
            );
        }

        @Test
        void initializeSteps_shouldCreateStepRecordsForAllCodes() {
            orchestrator.initializeSteps(createMessage());

            ArgumentCaptor<ProcessingStep> captor = ArgumentCaptor.forClass(ProcessingStep.class);
            verify(stepRepository, times(StepCode.values().length)).save(captor.capture());

            List<ProcessingStep> savedSteps = captor.getAllValues();
            assertEquals(StepCode.values().length, savedSteps.size());

            // All should be PENDING
            assertTrue(savedSteps.stream()
                    .allMatch(s -> s.getStatus() == StepStatus.PENDING));
        }
    }

    @Nested
    @DisplayName("executePipeline")
    class ExecutePipelineTests {

        @Test
        void executePipeline_allStepsSucceed_shouldCompleteMessage() {
            List<PipelineStep> allSteps = java.util.Arrays.stream(StepCode.values())
                    .map(code -> mockStep(code, true))
                    .toList();

            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository, allSteps
            );

            // Stub stepRepository.findByMarketMessageIdAndStepCode for status updates
            when(stepRepository.findByMarketMessageIdAndStepCode(anyLong(), any()))
                    .thenReturn(Optional.of(new ProcessingStep()));

            MarketMessage msg = createMessage();
            PipelineContext ctx = orchestrator.executePipeline(msg);

            assertEquals(MessageStatus.COMPLETED, msg.getStatus());
            assertNotNull(msg.getCompletedAt());
            assertFalse(ctx.isHalted());
        }

        @Test
        void executePipeline_earlyStepFails_shouldHaltAndSetFailed() {
            PipelineStep failStep = mockStep(StepCode.STEP_1B, false);
            PipelineStep goodStep = mockStep(StepCode.STEP_1A, true);

            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository,
                    List.of(goodStep, failStep)
            );

            when(stepRepository.findByMarketMessageIdAndStepCode(anyLong(), any()))
                    .thenReturn(Optional.of(new ProcessingStep()));

            MarketMessage msg = createMessage();
            PipelineContext ctx = orchestrator.executePipeline(msg);

            assertEquals(MessageStatus.FAILED, msg.getStatus());
            assertTrue(ctx.isHalted());
        }

        @Test
        void executePipeline_stepThrowsException_shouldHaltAndSetFailed() {
            PipelineStep exStep = mock(PipelineStep.class);
            when(exStep.getStepCode()).thenReturn(StepCode.STEP_1A);
            when(exStep.execute(any())).thenThrow(new RuntimeException("Unexpected error"));

            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository,
                    List.of(exStep)
            );

            when(stepRepository.findByMarketMessageIdAndStepCode(anyLong(), any()))
                    .thenReturn(Optional.of(new ProcessingStep()));

            MarketMessage msg = createMessage();
            PipelineContext ctx = orchestrator.executePipeline(msg);

            assertTrue(ctx.isHalted());
            assertTrue(ctx.getErrors().stream()
                    .anyMatch(e -> e.contains("Unexpected error")));
        }
    }

    @Nested
    @DisplayName("updateStepStatus")
    class UpdateStepStatusTests {

        private PipelineOrchestrator orchestrator;

        @BeforeEach
        void setUp() {
            orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository, List.of()
            );
        }

        @Test
        void updateStepStatus_inProgress_shouldSetStartedAt() {
            ProcessingStep step = new ProcessingStep();
            when(stepRepository.findByMarketMessageIdAndStepCode(1L, StepCode.STEP_1A))
                    .thenReturn(Optional.of(step));

            orchestrator.updateStepStatus(1L, StepCode.STEP_1A, StepStatus.IN_PROGRESS, null, null);

            assertNotNull(step.getStartedAt());
            assertEquals(StepStatus.IN_PROGRESS, step.getStatus());
            verify(stepRepository).save(step);
        }

        @Test
        void updateStepStatus_completed_shouldSetCompletedAt() {
            ProcessingStep step = new ProcessingStep();
            when(stepRepository.findByMarketMessageIdAndStepCode(1L, StepCode.STEP_1A))
                    .thenReturn(Optional.of(step));

            orchestrator.updateStepStatus(1L, StepCode.STEP_1A, StepStatus.COMPLETED, "Done", null);

            assertNotNull(step.getCompletedAt());
            assertEquals("Done", step.getResultMessage());
        }

        @Test
        void updateStepStatus_failed_shouldSetErrorMessage() {
            ProcessingStep step = new ProcessingStep();
            when(stepRepository.findByMarketMessageIdAndStepCode(1L, StepCode.STEP_1A))
                    .thenReturn(Optional.of(step));

            orchestrator.updateStepStatus(1L, StepCode.STEP_1A, StepStatus.FAILED, null, "Fout!");

            assertEquals("Fout!", step.getErrorMessage());
            assertEquals(StepStatus.FAILED, step.getStatus());
        }

        @Test
        void updateStepStatus_stepNotFound_shouldDoNothing() {
            when(stepRepository.findByMarketMessageIdAndStepCode(anyLong(), any()))
                    .thenReturn(Optional.empty());

            // Should not throw
            orchestrator.updateStepStatus(1L, StepCode.STEP_1A, StepStatus.COMPLETED, null, null);
            verify(stepRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("saveMessage")
    class SaveMessageTests {

        @Test
        void saveMessage_shouldDelegateToRepository() {
            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository, List.of()
            );

            MarketMessage msg = createMessage();
            orchestrator.saveMessage(msg);

            verify(messageRepository).save(msg);
        }
    }

    @Nested
    @DisplayName("logStep")
    class LogStepTests {

        @Test
        void logStep_shouldSaveProcessingLog() {
            PipelineOrchestrator orchestrator = new PipelineOrchestrator(
                    messageRepository, stepRepository, logRepository, List.of()
            );

            MarketMessage msg = createMessage();
            orchestrator.logStep(msg, StepCode.STEP_1A, "INFO", "Test log");

            verify(logRepository).save(any());
        }
    }

    // Helper to create mock PipelineStep
    private PipelineStep mockStep(StepCode code, boolean success) {
        PipelineStep step = mock(PipelineStep.class);
        when(step.getStepCode()).thenReturn(code);
        lenient().when(step.execute(any())).thenReturn(
                success ? StepResult.success("OK") : StepResult.failure("Failed")
        );
        return step;
    }
}
