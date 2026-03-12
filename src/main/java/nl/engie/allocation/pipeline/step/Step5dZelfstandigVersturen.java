package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 5D: Zelfstandig versturen uitgaande berichten - Independently send outgoing messages.
 */
@Component
public class Step5dZelfstandigVersturen implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step5dZelfstandigVersturen.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_5D;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        log.info("[5D] Controle op zelfstandig te versturen uitgaande berichten");
        return StepResult.skipped("Geen aanvullende uitgaande berichten");
    }
}
