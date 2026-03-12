package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 2A: Classificeer van berichttype - Classify the message type.
 */
@Component
public class Step2aClassificeer implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step2aClassificeer.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_2A;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        MessageType type = context.getDetectedMessageType();
        if (type == null) {
            type = context.getMessage().getMessageType();
        }

        if (type == null) {
            return StepResult.failure("Berichttype niet geïdentificeerd - kan niet classificeren");
        }

        String classification = switch (type) {
            case ALLOCATION_SERIES -> "INDIVIDUEEL_ALLOCATIEPUNT";
            case AGGREGATED_ALLOCATION_SERIES -> "GEAGGREGEERD_ALLOCATIE";
            case ALLOCATION_FACTOR_SERIES -> "RCF_PROFIELFRACTIES";
            case MANUAL_ENTRY -> "HANDMATIG";
        };

        context.setAttribute("classification", classification);
        log.info("[2A] Bericht geclassificeerd als: {}", classification);
        return StepResult.success("Classificatie: " + classification);
    }
}
