package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Step 5C: Logging verzendtijd - Log the sending timestamp.
 */
@Component
public class Step5cLoggingVerzendtijd implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step5cLoggingVerzendtijd.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_5C;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        LocalDateTime sendTime = LocalDateTime.now();
        context.setAttribute("responseSentAt", sendTime);

        log.info("[5C] Verzendtijd gelogd: {}", sendTime);
        return StepResult.success("Verzendtijd gelogd: " + sendTime);
    }
}
