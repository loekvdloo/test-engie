package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1A: Ontvangen marktbericht - Receive the market message.
 */
@Component
public class Step1aOntvangBericht implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1aOntvangBericht.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1A;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();

        if (message.getXmlContent() == null || message.getXmlContent().isBlank()) {
            return StepResult.failure("Geen berichtinhoud ontvangen");
        }

        log.info("[1A] Marktbericht ontvangen: UUID={}", message.getMessageUuid());
        return StepResult.success("Marktbericht succesvol ontvangen");
    }
}
