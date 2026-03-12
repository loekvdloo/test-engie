package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.DeliveryRecord;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.DeliveryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 6B: Vastleggen afleverstatus - Record delivery status.
 */
@Component
public class Step6bAfleverstatus implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step6bAfleverstatus.class);

    private final DeliveryRecordRepository deliveryRecordRepository;

    public Step6bAfleverstatus(DeliveryRecordRepository deliveryRecordRepository) {
        this.deliveryRecordRepository = deliveryRecordRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_6B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();

        var recordOpt = deliveryRecordRepository.findByMarketMessageId(message.getId());

        String deliveryStatus;
        if (context.isNack()) {
            deliveryStatus = "NACK_SENT";
        } else if (recordOpt.isPresent()) {
            deliveryStatus = "DELIVERED";
        } else {
            deliveryStatus = "NOT_APPLICABLE";
        }

        if (recordOpt.isPresent()) {
            DeliveryRecord record = recordOpt.get();
            record.setDeliveryStatus(deliveryStatus);
            deliveryRecordRepository.save(record);
        } else {
            DeliveryRecord record = DeliveryRecord.builder()
                    .marketMessage(message)
                    .deliveryTarget("RAW_LAYER")
                    .deliveryStatus(deliveryStatus)
                    .build();
            deliveryRecordRepository.save(record);
        }

        message.setStatus(MessageStatus.DELIVERED);

        log.info("[6B] Afleverstatus vastgelegd: {}", deliveryStatus);
        return StepResult.success("Afleverstatus: " + deliveryStatus);
    }
}
