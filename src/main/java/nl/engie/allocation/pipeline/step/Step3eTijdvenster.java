package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Step 3E: Tijdvenster-validaties - Time window validations.
 */
@Component
public class Step3eTijdvenster implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3eTijdvenster.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3E;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        // Parse startDateTime / endDateTime from XML if not yet set on the entity
        String xml = message.getXmlContent();
        if (xml != null) {
            if (message.getStartDateTime() == null) {
                LocalDateTime parsed = parseDateTime(extractValue(xml, "startDateTime"));
                if (parsed != null) message.setStartDateTime(parsed);
            }
            if (message.getEndDateTime() == null) {
                LocalDateTime parsed = parseDateTime(extractValue(xml, "endDateTime"));
                if (parsed != null) message.setEndDateTime(parsed);
            }
        }

        if (message.getStartDateTime() != null && message.getEndDateTime() != null) {
            if (message.getEndDateTime().isBefore(message.getStartDateTime())) {
                context.addValidationError(ErrorCode.E_663.getCode(),
                        ErrorCode.E_663.getFoutmelding());
            }
            if (message.getStartDateTime().isAfter(LocalDateTime.now())) {
                context.addValidationError(ErrorCode.E_772.getCode(),
                        ErrorCode.E_772.getFoutmelding());
            }
        }

        if (message.getReceivedAt() != null) {
            LocalDateTime maxAge = LocalDateTime.now().minusDays(30);
            if (message.getReceivedAt().isBefore(maxAge)) {
                context.addValidationError(ErrorCode.E_763.getCode(),
                        ErrorCode.E_763.getFoutmelding());
            }
        }

        log.info("[3E] Tijdvenster-validaties uitgevoerd");
        return StepResult.success("Tijdvenster-validaties voltooid");
    }

    private String extractValue(String xml, String tagName) {
        String start = "<" + tagName + ">";
        String end = "</" + tagName + ">";
        int s = xml.indexOf(start);
        int e = xml.indexOf(end);
        if (s >= 0 && e > s) return xml.substring(s + start.length(), e).trim();
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) return null;
        try {
            String v = value.endsWith("Z") ? value.substring(0, value.length() - 1) : value;
            return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
