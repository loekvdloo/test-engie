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
 * Step 2B: Bepalen prioriteit per berichttype - Determine priority based on message type.
 */
@Component
public class Step2bBepalenPrioriteit implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step2bBepalenPrioriteit.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_2B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        MessageType type = context.getDetectedMessageType();
        if (type == null) type = context.getMessage().getMessageType();

        int priority = switch (type != null ? type : MessageType.MANUAL_ENTRY) {
            case ALLOCATION_SERIES -> 1;
            case AGGREGATED_ALLOCATION_SERIES -> 2;
            case ALLOCATION_FACTOR_SERIES -> 3;
            case MANUAL_ENTRY -> 5;
        };

        context.setAssignedPriority(priority);
        context.getMessage().setPriority(priority);

        log.info("[2B] Prioriteit bepaald: {} voor type: {}", priority, type);
        return StepResult.success("Prioriteit: " + priority);
    }
}
