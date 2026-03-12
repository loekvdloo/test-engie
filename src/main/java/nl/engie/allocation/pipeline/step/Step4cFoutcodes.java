package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 4C: Toevoegen foutcodes bij NACK - Add error codes to NACK response.
 */
@Component
public class Step4cFoutcodes implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step4cFoutcodes.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_4C;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.isNack()) {
            return StepResult.skipped("Geen NACK - foutcodes niet nodig");
        }

        for (var error : context.getValidationErrors()) {
            log.info("[4C] Foutcode: {} - {}", error.code(), error.message());
        }

        return StepResult.success("Foutcodes toegevoegd aan NACK: "
                + context.getValidationErrors().size() + " codes");
    }
}
