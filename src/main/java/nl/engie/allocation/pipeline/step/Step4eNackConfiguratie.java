package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Step 4E: Configuratie: NACK wel/niet intern doorzetten.
 */
@Component
public class Step4eNackConfiguratie implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step4eNackConfiguratie.class);

    @Value("${pipeline.forward-nack-internally:false}")
    private boolean forwardNackInternally;

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_4E;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isNack()) {
            return StepResult.skipped("Geen NACK - configuratie niet van toepassing");
        }

        context.setAttribute("forwardNackInternally", forwardNackInternally);

        if (forwardNackInternally) {
            log.info("[4E] NACK wordt intern doorgezet (configuratie: AAN)");
            return StepResult.success("NACK wordt intern doorgezet");
        } else {
            log.info("[4E] NACK wordt NIET intern doorgezet (configuratie: UIT)");
            return StepResult.success("NACK wordt niet intern doorgezet");
        }
    }
}
