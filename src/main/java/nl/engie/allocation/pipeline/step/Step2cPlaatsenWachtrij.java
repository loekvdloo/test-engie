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
 * Step 2C: Plaatsen in wachtrij (event-driven) - Place message in processing queue.
 */
@Component
public class Step2cPlaatsenWachtrij implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step2cPlaatsenWachtrij.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_2C;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        context.setAttribute("queued", true);
        context.setAttribute("queuedAt", LocalDateTime.now());

        log.info("[2C] Bericht in wachtrij geplaatst met prioriteit: {}",
                context.getAssignedPriority());
        return StepResult.success("Bericht in wachtrij geplaatst");
    }
}
