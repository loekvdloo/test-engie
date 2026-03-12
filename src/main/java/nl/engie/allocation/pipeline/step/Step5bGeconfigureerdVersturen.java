package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 5B: Geconfigureerd respons versturen - Send configured response.
 */
@Component
public class Step5bGeconfigureerdVersturen implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step5bGeconfigureerdVersturen.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_5B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        Boolean forwardNack = context.getAttribute("forwardNackInternally", Boolean.class);
        if (context.isNack() && !Boolean.TRUE.equals(forwardNack)) {
            log.info("[5B] NACK niet intern doorgezet (configuratie)");
            return StepResult.skipped("NACK niet intern doorgezet");
        }

        log.info("[5B] Geconfigureerde respons verwerkt");
        return StepResult.success("Geconfigureerde respons verwerkt");
    }
}
