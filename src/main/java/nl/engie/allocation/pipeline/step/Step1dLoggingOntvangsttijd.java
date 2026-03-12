package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1D: Logging van ontvangsttijd - Log the receipt timestamp.
 */
@Component
public class Step1dLoggingOntvangsttijd implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1dLoggingOntvangsttijd.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1D;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        var receivedAt = message.getReceivedAt();

        log.info("[1D] Ontvangsttijd gelogd: {}", receivedAt);
        return StepResult.success("Ontvangsttijd gelogd: " + receivedAt);
    }
}
