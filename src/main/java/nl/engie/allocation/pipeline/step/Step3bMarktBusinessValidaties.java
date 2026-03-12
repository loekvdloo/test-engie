package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3B: Uitvoeren marktbusiness validaties - Execute market business validations.
 */
@Component
public class Step3bMarktBusinessValidaties implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3bMarktBusinessValidaties.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        // Validate product type
        if (xml.contains("<product>") || xml.contains("<identification>")) {
            if (!xml.contains("023") && !xml.contains("8716867000016")) {
                context.addValidationError("BIZ001",
                        "Ongeldige productsoort - verwacht elektriciteit (023)");
            }
        }

        // Validate allocation group for aggregated messages
        if (message.getMessageType() != null) {
            switch (message.getMessageType()) {
                case AGGREGATED_ALLOCATION_SERIES -> {
                    if (!xml.contains("PRF") && !xml.contains("TMT")
                            && !xml.contains("SMA") && !xml.contains("NVL")
                            && !xml.contains("DIM")) {
                        context.addValidationError("BIZ002",
                                "Ongeldige allocatiegroep voor geaggregeerd bericht");
                    }
                }
                case ALLOCATION_FACTOR_SERIES -> {
                    if (!xml.contains("PT15M")) {
                        context.addValidationError("BIZ003",
                                "Resolutie moet PT15M zijn voor elektriciteit");
                    }
                }
                default -> {}
            }
        }

        int errorCount = context.getValidationErrors().size();
        log.info("[3B] Marktbusiness validaties uitgevoerd - {} fouten", errorCount);
        return StepResult.success("Business validaties voltooid: " + errorCount + " fouten");
    }
}
