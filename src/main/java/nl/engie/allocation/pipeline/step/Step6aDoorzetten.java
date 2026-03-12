package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.DeliveryRecord;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.DeliveryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Step 6A: Doorzetten origineel bericht naar raw-layer.
 */
@Component
public class Step6aDoorzetten implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step6aDoorzetten.class);

    private final DeliveryRecordRepository deliveryRecordRepository;

    public Step6aDoorzetten(DeliveryRecordRepository deliveryRecordRepository) {
        this.deliveryRecordRepository = deliveryRecordRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_6A;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();

        Boolean forwardNack = context.getAttribute("forwardNackInternally", Boolean.class);
        if (context.isNack() && !Boolean.TRUE.equals(forwardNack)) {
            log.info("[6A] NACK bericht - niet doorgezet naar raw-layer");
            return StepResult.skipped("NACK bericht niet doorgezet");
        }

        String rawPath = String.format("raw/allocation/%s/%s/%s.xml",
                message.getMessageType() != null ? message.getMessageType().name().toLowerCase() : "unknown",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                message.getMessageUuid());

        DeliveryRecord record = DeliveryRecord.builder()
                .marketMessage(message)
                .deliveryTarget("RAW_LAYER")
                .deliveryStatus("DELIVERED")
                .rawLayerPath(rawPath)
                .deliveredAt(LocalDateTime.now())
                .build();
        deliveryRecordRepository.save(record);

        log.info("[6A] Bericht doorgezet naar raw-layer: {}", rawPath);
        return StepResult.success("Doorgezet naar raw-layer: " + rawPath);
    }
}
