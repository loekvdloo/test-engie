package nl.engie.allocation.service;

import nl.engie.allocation.dto.MessageStatusResponse;
import nl.engie.allocation.dto.MessageSubmitRequest;
import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.MarketResponse;
import nl.engie.allocation.model.entity.ProcessingStep;
import nl.engie.allocation.model.enums.*;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineOrchestrator;
import nl.engie.allocation.repository.MarketMessageRepository;
import nl.engie.allocation.repository.MarketResponseRepository;
import nl.engie.allocation.repository.ProcessingStepRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketMessageServiceTest {

    @Mock private MarketMessageRepository messageRepository;
    @Mock private ProcessingStepRepository stepRepository;
    @Mock private MarketResponseRepository responseRepository;
    @Mock private ValidationResultRepository validationResultRepository;
    @Mock private PipelineOrchestrator pipelineOrchestrator;

    private MarketMessageService service;

    @BeforeEach
    void setUp() {
        service = new MarketMessageService(
                messageRepository, stepRepository, responseRepository,
                validationResultRepository, pipelineOrchestrator
        );
    }

    private MarketMessage createMessage(String uuid) {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid(uuid)
                .xmlContent("<test/>")
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .messageType(MessageType.ALLOCATION_SERIES)
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("submitMessage")
    class SubmitMessageTests {

        @Test
        void submitMessage_shouldSaveAndReturnUuid() {
            when(messageRepository.save(any(MarketMessage.class)))
                    .thenAnswer(inv -> {
                        MarketMessage m = inv.getArgument(0);
                        m.setId(1L);
                        return m;
                    });
            when(messageRepository.findById(1L)).thenReturn(
                    Optional.of(createMessage("any"))
            );

            MessageSubmitRequest request = new MessageSubmitRequest(
                    "<AllocationSeries/>", false, "EAN123"
            );

            String uuid = service.submitMessage(request);

            assertNotNull(uuid);
            assertFalse(uuid.isEmpty());
            verify(messageRepository).save(any(MarketMessage.class));
            verify(pipelineOrchestrator).initializeSteps(any());
        }

        @Test
        void submitMessage_withManualEntry_shouldSetFlagOnMessage() {
            when(messageRepository.save(any(MarketMessage.class)))
                    .thenAnswer(inv -> {
                        MarketMessage m = inv.getArgument(0);
                        m.setId(1L);
                        return m;
                    });
            when(messageRepository.findById(1L)).thenReturn(
                    Optional.of(createMessage("any"))
            );

            MessageSubmitRequest request = new MessageSubmitRequest(
                    "<test/>", true, null
            );

            service.submitMessage(request);

            ArgumentCaptor<MarketMessage> captor = ArgumentCaptor.forClass(MarketMessage.class);
            verify(messageRepository).save(captor.capture());
            assertTrue(captor.getValue().getIsManualEntry());
        }
    }

    @Nested
    @DisplayName("processMessage")
    class ProcessMessageTests {

        @Test
        void processMessage_existingId_shouldExecutePipeline() {
            MarketMessage msg = createMessage("test-uuid");
            when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));

            service.processMessage(1L);

            verify(pipelineOrchestrator).executePipeline(msg);
        }

        @Test
        void processMessage_unknownId_shouldThrowException() {
            when(messageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> service.processMessage(999L));
        }
    }

    @Nested
    @DisplayName("getMessageStatus")
    class GetMessageStatusTests {

        @Test
        void getMessageStatus_shouldReturnFullStatus() {
            MarketMessage msg = createMessage("my-uuid");
            msg.setStatus(MessageStatus.COMPLETED);
            msg.setCurrentStep(StepCode.STEP_6B);
            msg.setPriority(1);
            when(messageRepository.findByMessageUuid("my-uuid")).thenReturn(Optional.of(msg));

            ProcessingStep step = ProcessingStep.builder()
                    .marketMessage(msg)
                    .stepCode(StepCode.STEP_1A)
                    .stepName("Ontvang Bericht")
                    .phaseName("Ontvangst")
                    .stepOrder(1)
                    .status(StepStatus.COMPLETED)
                    .build();
            when(stepRepository.findByMarketMessageIdOrderByStepOrderAsc(1L))
                    .thenReturn(List.of(step));

            MarketResponse resp = new MarketResponse();
            resp.setResponseType(ResponseType.ACK);
            resp.setXmlResponse("<ack/>");
            when(responseRepository.findByMarketMessageId(1L)).thenReturn(Optional.of(resp));
            when(validationResultRepository.findByMarketMessageIdAndIsValidFalse(1L))
                    .thenReturn(List.of());

            MessageStatusResponse result = service.getMessageStatus("my-uuid");

            assertEquals("my-uuid", result.messageUuid());
            assertEquals("COMPLETED", result.status());
            assertEquals("STEP_6B", result.currentStep());
            assertEquals(1, result.priority());
            assertEquals("ACK", result.responseType());
            assertEquals("<ack/>", result.responseXml());
            assertEquals(1, result.steps().size());
            assertEquals("STEP_1A", result.steps().get(0).stepCode());
        }

        @Test
        void getMessageStatus_withoutResponse_shouldReturnNullResponseFields() {
            MarketMessage msg = createMessage("no-resp-uuid");
            when(messageRepository.findByMessageUuid("no-resp-uuid")).thenReturn(Optional.of(msg));
            when(stepRepository.findByMarketMessageIdOrderByStepOrderAsc(1L))
                    .thenReturn(List.of());
            when(responseRepository.findByMarketMessageId(1L)).thenReturn(Optional.empty());
            when(validationResultRepository.findByMarketMessageIdAndIsValidFalse(1L))
                    .thenReturn(List.of());

            MessageStatusResponse result = service.getMessageStatus("no-resp-uuid");

            assertNull(result.responseType());
            assertNull(result.responseXml());
        }

        @Test
        void getMessageStatus_unknownUuid_shouldThrowException() {
            when(messageRepository.findByMessageUuid("unknown"))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> service.getMessageStatus("unknown"));
        }
    }

    @Nested
    @DisplayName("getAllMessages")
    class GetAllMessagesTests {

        @Test
        void getAllMessages_shouldReturnListOfOverviews() {
            MarketMessage m1 = createMessage("uuid-1");
            MarketMessage m2 = createMessage("uuid-2");
            m2.setId(2L);
            when(messageRepository.findAll()).thenReturn(List.of(m1, m2));
            when(messageRepository.findByMessageUuid("uuid-1")).thenReturn(Optional.of(m1));
            when(messageRepository.findByMessageUuid("uuid-2")).thenReturn(Optional.of(m2));
            when(stepRepository.findByMarketMessageIdOrderByStepOrderAsc(anyLong())).thenReturn(List.of());
            when(responseRepository.findByMarketMessageId(anyLong())).thenReturn(Optional.empty());
            when(validationResultRepository.findByMarketMessageIdAndIsValidFalse(anyLong())).thenReturn(List.of());

            List<MessageStatusResponse> result = service.getAllMessages();

            assertEquals(2, result.size());
        }

        @Test
        void getAllMessages_empty_shouldReturnEmptyList() {
            when(messageRepository.findAll()).thenReturn(List.of());

            List<MessageStatusResponse> result = service.getAllMessages();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMessagesByStatus")
    class GetMessagesByStatusTests {

        @Test
        void getMessagesByStatus_shouldReturnMatchingMessages() {
            MarketMessage msg = createMessage("complete-uuid");
            msg.setStatus(MessageStatus.COMPLETED);
            when(messageRepository.findByStatus(MessageStatus.COMPLETED))
                    .thenReturn(List.of(msg));
            when(messageRepository.findByMessageUuid("complete-uuid"))
                    .thenReturn(Optional.of(msg));
            when(stepRepository.findByMarketMessageIdOrderByStepOrderAsc(1L))
                    .thenReturn(List.of());
            when(responseRepository.findByMarketMessageId(1L))
                    .thenReturn(Optional.empty());
            when(validationResultRepository.findByMarketMessageIdAndIsValidFalse(1L))
                    .thenReturn(List.of());

            List<MessageStatusResponse> result =
                    service.getMessagesByStatus(MessageStatus.COMPLETED);

            assertEquals(1, result.size());
            assertEquals("COMPLETED", result.get(0).status());
        }
    }

    @Nested
    @DisplayName("reprocessMessage")
    class ReprocessMessageTests {

        @Test
        void reprocessMessage_failedMessage_shouldResetAndReprocess() {
            MarketMessage msg = createMessage("failed-uuid");
            msg.setStatus(MessageStatus.FAILED);
            when(messageRepository.findByMessageUuid("failed-uuid"))
                    .thenReturn(Optional.of(msg));
            when(stepRepository.findByMarketMessageIdOrderByStepOrderAsc(1L))
                    .thenReturn(List.of());
            when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));

            String result = service.reprocessMessage("failed-uuid");

            assertEquals("failed-uuid", result);
            assertEquals(MessageStatus.RECEIVED, msg.getStatus());
            assertNull(msg.getCurrentStep());
            assertNull(msg.getCompletedAt());
            verify(messageRepository).save(msg); // reset save
            verify(pipelineOrchestrator).initializeSteps(msg);
            verify(pipelineOrchestrator).executePipeline(msg);
        }

        @Test
        void reprocessMessage_parkedMessage_shouldResetAndReprocess() {
            MarketMessage msg = createMessage("parked-uuid");
            msg.setStatus(MessageStatus.PARKED);
            when(messageRepository.findByMessageUuid("parked-uuid"))
                    .thenReturn(Optional.of(msg));
            when(stepRepository.findByMarketMessageIdOrderByStepOrderAsc(1L))
                    .thenReturn(List.of());
            when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));

            String result = service.reprocessMessage("parked-uuid");

            assertEquals("parked-uuid", result);
            verify(pipelineOrchestrator).executePipeline(msg);
        }

        @Test
        void reprocessMessage_completedMessage_shouldThrowException() {
            MarketMessage msg = createMessage("completed-uuid");
            msg.setStatus(MessageStatus.COMPLETED);
            when(messageRepository.findByMessageUuid("completed-uuid"))
                    .thenReturn(Optional.of(msg));

            assertThrows(RuntimeException.class,
                    () -> service.reprocessMessage("completed-uuid"));
        }

        @Test
        void reprocessMessage_unknownUuid_shouldThrowException() {
            when(messageRepository.findByMessageUuid("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> service.reprocessMessage("nonexistent"));
        }
    }
}
