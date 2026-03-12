package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 2E: Uitval opnieuw verwerken - Handle reprocessing of failed messages.
 */
@Component
public class Step2eUitvalOpnieuw implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step2eUitvalOpnieuw.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_2E;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        Boolean isRetry = context.getAttribute("isRetry", Boolean.class);

        if (Boolean.TRUE.equals(isRetry)) {
            log.info("[2E] Bericht wordt opnieuw verwerkt");
            context.setAttribute("retryCount",
                    context.getAttribute("retryCount", Integer.class) != null
                            ? context.getAttribute("retryCount", Integer.class) + 1
                            : 1);
            return StepResult.success("Herverwerking gestart");
        }

        return StepResult.skipped("Geen herverwerking nodig");
    }
}
