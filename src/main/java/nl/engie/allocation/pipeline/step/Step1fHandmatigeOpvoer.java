package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1F: Handmatige opvoer berichten - Handle manually entered messages.
 */
@Component
public class Step1fHandmatigeOpvoer implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1fHandmatigeOpvoer.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1F;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();

        if (Boolean.TRUE.equals(message.getIsManualEntry())) {
            log.info("[1F] Handmatig opgevoerd bericht gemarkeerd");
            context.setAttribute("manualEntry", true);
            return StepResult.success("Handmatig opgevoerd bericht verwerkt");
        }

        return StepResult.skipped("Geen handmatig bericht - stap overgeslagen");
    }
}
