package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 2D: Uitzondering parkeren - Park exceptions for later review.
 */
@Component
public class Step2dUitzonderingParkeren implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step2dUitzonderingParkeren.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_2D;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isTechnicallyValid()) {
            context.setParked(true);
            context.getMessage().setStatus(MessageStatus.PARKED);
            log.info("[2D] Bericht geparkeerd wegens technische validatiefouten");
            return StepResult.success("Bericht geparkeerd voor handmatige review");
        }

        log.info("[2D] Geen uitzonderingen - bericht niet geparkeerd");
        return StepResult.skipped("Geen uitzonderingen gevonden");
    }
}
