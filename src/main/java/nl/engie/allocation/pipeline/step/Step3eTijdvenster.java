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
}
