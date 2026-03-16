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

        // 1. UUID format validation for mRID / MessageID
        if (xml.contains("<mRID>")) {
            String mrid = extractValue(xml, "mRID");
            if (mrid != null && !mrid.matches(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                context.addValidationError(ErrorCode.E_669.getCode(),
                        ErrorCode.E_669.getFoutmelding() + ": " + mrid);
            }
        }

        // 2. DateTime format validation (ISO 8601: YYYY-MM-DDThh:mm:ssZ)
        for (String field : new String[]{"startDateTime", "endDateTime"}) {
            String value = extractValue(xml, field);
            if (value != null && !value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
                context.addValidationError(ErrorCode.E_999.getCode(),
                        field + " is niet in ISO 8601 formaat: " + value);
            }
        }

        // 3. Volume values must have 3 decimal places
        String quantity = extractValue(xml, "quantity");
        if (quantity != null && !quantity.matches("-?\\d+\\.\\d{3}")) {
            context.addValidationError(ErrorCode.E_776.getCode(),
                    ErrorCode.E_776.getFoutmelding() + ": " + quantity);
        }

        // 4. EAN-18 code validation (must be exactly 18 digits)
        String senderEan = extractValue(xml, "sender_MarketParticipant.mRID");
        if (senderEan != null && !senderEan.matches("\\d{18}")) {
            context.addValidationError(ErrorCode.E_651.getCode(),
                    ErrorCode.E_651.getFoutmelding() + ": " + senderEan);
        }
        String receiverEan = extractValue(xml, "receiver_MarketParticipant.mRID");
        if (receiverEan != null && !receiverEan.matches("\\d{18}")) {
            context.addValidationError(ErrorCode.E_651.getCode(),
                    ErrorCode.E_651.getFoutmelding() + ": " + receiverEan);
        }

        // E_670: eerder ontvangen bericht met dit kenmerk
        if (xml.contains("<duplicaatKenmerk>JA</duplicaatKenmerk>")) {
            String mrid = extractValue(xml, "mRID");
            context.addValidationError(ErrorCode.E_670.getCode(),
                    ErrorCode.E_670.getFoutmelding() + (mrid != null ? ": " + mrid : ""));
        }

        // E_704: recentere creatie datum/tijdstempel al ontvangen
        if (xml.contains("<isLatestVersion>NEEN</isLatestVersion>")) {
            context.addValidationError(ErrorCode.E_704.getCode(), ErrorCode.E_704.getFoutmelding());
        }

        // E_769: duplicate allocatierun identificatie
        if (xml.contains("<allocatieRunId>")) {
            String runId = extractValue(xml, "allocatieRunId");
            if (runId != null && runId.contains("REEDS-VERWERKT")) {
                context.addValidationError(ErrorCode.E_769.getCode(),
                        ErrorCode.E_769.getFoutmelding() + ": " + runId);
            }
        }

        // E_774: factor heeft onjuist aantal decimalen
        java.util.regex.Matcher factorMatcher = java.util.regex.Pattern
                .compile("<factor>(-?[\\d.]+)</factor>").matcher(xml);
        while (factorMatcher.find()) {
            String factorVal = factorMatcher.group(1);
            if (!factorVal.matches("-?\\d+\\.\\d{3}")) {
                context.addValidationError(ErrorCode.E_774.getCode(),
                        ErrorCode.E_774.getFoutmelding() + ": " + factorVal);
                break;
            }
        }

        // E_780: CorrelationID mismatch
        if (xml.contains("<correlationID>MISMATCH</correlationID>")) {
            context.addValidationError(ErrorCode.E_780.getCode(), ErrorCode.E_780.getFoutmelding());
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
