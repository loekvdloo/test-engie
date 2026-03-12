package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3G: Herbruikbare validatieregels - Execute reusable validation rules.
 */
@Component
public class Step3gHerbruikbareRegels implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3gHerbruikbareRegels.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3G;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        // 1. UUID format validation for mRID
        if (xml.contains("<mRID>")) {
            String mrid = extractValue(xml, "mRID");
            if (mrid != null && !mrid.matches(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                context.addValidationError("HBR001",
                        "mRID is geen geldig UUID formaat: " + mrid);
            }
        }

        // 2. DateTime format validation (ISO 8601: YYYY-MM-DDThh:mm:ssZ)
        for (String field : new String[]{"startDateTime", "endDateTime"}) {
            String value = extractValue(xml, field);
            if (value != null && !value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
                context.addValidationError("HBR002",
                        field + " is niet in ISO 8601 formaat: " + value);
            }
        }

        // 3. Volume values must have 3 decimal places
        String quantity = extractValue(xml, "quantity");
        if (quantity != null && !quantity.matches("-?\\d+\\.\\d{3}")) {
            context.addWarning("Volume waarde heeft niet exact 3 decimalen: " + quantity);
        }

        log.info("[3G] Herbruikbare validatieregels uitgevoerd");
        return StepResult.success("Herbruikbare validatieregels voltooid");
    }

    private String extractValue(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + startTag.length(), end).trim();
        }
        return null;
    }
}
