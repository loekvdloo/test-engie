package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.ValidationResult;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.ValidationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 4D: Vastleggen validatieresultaat - Record the validation result in database.
 */
@Component
public class Step4dVastleggenResultaat implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step4dVastleggenResultaat.class);

    private final ValidationResultRepository validationResultRepository;

    public Step4dVastleggenResultaat(ValidationResultRepository validationResultRepository) {
        this.validationResultRepository = validationResultRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_4D;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();

        // Save individual validation error entries (specific error codes from steps 3A-3G)
        if (context.isNack()) {
            for (var error : context.getValidationErrors()) {
                ValidationResult errorResult = ValidationResult.builder()
                        .marketMessage(message)
                        .ruleCode(error.code())
                        .isValid(false)
                        .errorCode(error.code())
                        .errorMessage(error.message())
                        .build();
                validationResultRepository.save(errorResult);
            }
        }

        // Save overall validation result
        ValidationResult overallResult = ValidationResult.builder()
                .marketMessage(message)
                .ruleCode("OVERALL")
                .isValid(!context.isNack())
                .errorCode(context.isNack() ? "VALIDATION_FAILED" : null)
                .errorMessage(context.isNack()
                        ? "Bericht heeft " + context.getValidationErrors().size() + " validatiefouten"
                        : "Alle validaties geslaagd")
                .build();
        validationResultRepository.save(overallResult);

        log.info("[4D] Validatieresultaat vastgelegd: {}",
                context.isNack() ? "NIET GELDIG" : "GELDIG");
        return StepResult.success("Validatieresultaat vastgelegd");
    }
}
