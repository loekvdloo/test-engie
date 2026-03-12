package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3C: Controle op verplichte velden - Check required fields.
 */
@Component
public class Step3cControleVerplicht implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3cControleVerplicht.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3C;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();
        int errorsBefore = context.getValidationErrors().size();

        String[] requiredElements = {
                "mRID",
                "product",
                "startDateTime",
                "endDateTime",
                "resolution"
        };

        for (String element : requiredElements) {
            if (!xml.contains("<" + element + ">") && !xml.contains("<" + element + " ")) {
                context.addValidationError(ErrorCode.E_999.getCode(),
                        "Verplicht veld ontbreekt: " + element);
            }
        }

        int newErrors = context.getValidationErrors().size() - errorsBefore;
        log.info("[3C] Controle verplichte velden: {} ontbrekende velden", newErrors);
        return StepResult.success("Verplichte velden gecontroleerd: " + newErrors + " ontbrekend");
    }
}
