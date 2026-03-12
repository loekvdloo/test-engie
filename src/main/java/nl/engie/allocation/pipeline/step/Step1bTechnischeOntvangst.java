package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Step 1B: Technische ontvangstbevestiging - Generate technical receipt confirmation.
 */
@Component
public class Step1bTechnischeOntvangst implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1bTechnischeOntvangst.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        String receiptId = UUID.randomUUID().toString();
        context.setAttribute("technicalReceiptId", receiptId);

        log.info("[1B] Technische ontvangstbevestiging gegenereerd: {}", receiptId);
        return StepResult.success("Technische ontvangstbevestiging: " + receiptId);
    }
}
