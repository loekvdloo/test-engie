package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.DeliveryRecord;
import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.DeliveryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Phase 6 steps: 6A and 6B.
 */
@ExtendWith(MockitoExtension.class)
class Phase6StepTests {

    private MarketMessage createMessage() {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid("test-uuid")
                .xmlContent("<test/>")
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .messageType(MessageType.ALLOCATION_SERIES)
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("Step 6A: Doorzetten naar raw-layer")
    class Step6aTests {

        @Mock
        private DeliveryRecordRepository deliveryRecordRepository;
        private Step6aDoorzetten step;

        @BeforeEach
        void setUp() {
            step = new Step6aDoorzetten(deliveryRecordRepository);
        }

        @Test
        void stepCode_shouldBeStep6A() {
            assertEquals(StepCode.STEP_6A, step.getStepCode());
        }

        @Test
        void execute_ackMessage_shouldDeliverToRawLayer() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(false);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());

            ArgumentCaptor<DeliveryRecord> captor = ArgumentCaptor.forClass(DeliveryRecord.class);
            verify(deliveryRecordRepository).save(captor.capture());
            assertEquals("RAW_LAYER", captor.getValue().getDeliveryTarget());
            assertEquals("DELIVERED", captor.getValue().getDeliveryStatus());
            assertTrue(captor.getValue().getRawLayerPath().startsWith("raw/allocation/"));
        }

        @Test
        void execute_nackWithoutForward_shouldBeSkipped() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            ctx.setAttribute("forwardNackInternally", false);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSkipped());
            verify(deliveryRecordRepository, never()).save(any());
        }

        @Test
        void execute_nackWithForward_shouldDeliver() {
            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            ctx.setAttribute("forwardNackInternally", true);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertFalse(result.isSkipped());
            verify(deliveryRecordRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Step 6B: Vastleggen afleverstatus")
    class Step6bTests {

        @Mock
        private DeliveryRecordRepository deliveryRecordRepository;
        private Step6bAfleverstatus step;

        @BeforeEach
        void setUp() {
            step = new Step6bAfleverstatus(deliveryRecordRepository);
        }

        @Test
        void stepCode_shouldBeStep6B() {
            assertEquals(StepCode.STEP_6B, step.getStepCode());
        }

        @Test
        void execute_withExistingRecord_shouldUpdateStatus() {
            DeliveryRecord record = new DeliveryRecord();
            record.setDeliveryStatus("PENDING");
            when(deliveryRecordRepository.findByMarketMessageId(1L)).thenReturn(Optional.of(record));

            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(false);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals(MessageStatus.DELIVERED, ctx.getMessage().getStatus());
            assertEquals("DELIVERED", record.getDeliveryStatus());
            verify(deliveryRecordRepository).save(record);
        }

        @Test
        void execute_nack_shouldSetNackSentStatus() {
            DeliveryRecord record = new DeliveryRecord();
            when(deliveryRecordRepository.findByMarketMessageId(1L)).thenReturn(Optional.of(record));

            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(true);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertEquals("NACK_SENT", record.getDeliveryStatus());
        }

        @Test
        void execute_withoutRecord_shouldCreateNewRecord() {
            when(deliveryRecordRepository.findByMarketMessageId(1L)).thenReturn(Optional.empty());

            PipelineContext ctx = new PipelineContext(createMessage());
            ctx.setNack(false);
            step.execute(ctx);

            ArgumentCaptor<DeliveryRecord> captor = ArgumentCaptor.forClass(DeliveryRecord.class);
            verify(deliveryRecordRepository).save(captor.capture());
            assertEquals("NOT_APPLICABLE", captor.getValue().getDeliveryStatus());
        }
    }
}
