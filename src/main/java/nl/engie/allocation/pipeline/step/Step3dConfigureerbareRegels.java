package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.ValidationResult;
import nl.engie.allocation.model.entity.ValidationRule;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.ValidationResultRepository;
import nl.engie.allocation.repository.ValidationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Step 3D: Validatieregels configureerbaar - Execute configurable validation rules.
 */
@Component
public class Step3dConfigureerbareRegels implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3dConfigureerbareRegels.class);

    private final ValidationRuleRepository validationRuleRepository;
    private final ValidationResultRepository validationResultRepository;

    public Step3dConfigureerbareRegels(ValidationRuleRepository validationRuleRepository,
                                       ValidationResultRepository validationResultRepository) {
        this.validationRuleRepository = validationRuleRepository;
        this.validationResultRepository = validationResultRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3D;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String messageType = message.getMessageType() != null
                ? message.getMessageType().name() : null;

        List<ValidationRule> rules = new ArrayList<>();
        if (messageType != null) {
            rules.addAll(validationRuleRepository.findByMessageTypeAndIsActiveTrue(messageType));
            rules.addAll(validationRuleRepository.findByIsActiveTrue().stream()
                    .filter(r -> r.getMessageType() == null || r.getMessageType().isBlank())
                    .toList());
        } else {
            rules.addAll(validationRuleRepository.findByIsActiveTrue());
        }

        int passed = 0;
        int failed = 0;
        for (ValidationRule rule : rules) {
            boolean valid = evaluateRule(rule, message.getXmlContent());

            ValidationResult result = ValidationResult.builder()
                    .marketMessage(message)
                    .validationRule(rule)
                    .ruleCode(rule.getRuleCode())
                    .isValid(valid)
                    .errorCode(valid ? null : rule.getErrorCode())
                    .errorMessage(valid ? null : rule.getErrorMessage())
                    .validatedAt(LocalDateTime.now())
                    .build();
            validationResultRepository.save(result);

            if (valid) {
                passed++;
            } else {
                failed++;
                context.addValidationError(rule.getErrorCode(), rule.getErrorMessage());
            }
        }

        log.info("[3D] Configureerbare regels: {} geslaagd, {} mislukt", passed, failed);
        return StepResult.success("Configureerbare validatie: " + passed
                + " geslaagd, " + failed + " mislukt");
    }

    private boolean evaluateRule(ValidationRule rule, String xml) {
        String expr = rule.getRuleExpression();

        if (expr.startsWith("CONTAINS:")) {
            return xml.contains(expr.substring("CONTAINS:".length()));
        } else if (expr.startsWith("NOT_CONTAINS:")) {
            return !xml.contains(expr.substring("NOT_CONTAINS:".length()));
        } else if (expr.startsWith("REGEX:")) {
            String pattern = expr.substring("REGEX:".length());
            return Pattern.compile(pattern).matcher(xml).find();
        } else if (expr.startsWith("LOOKUP:") || expr.startsWith("CHECK:")) {
            // LOOKUP and CHECK rules are handled by dedicated steps (3A, 3B, 3G)
            return true;
        }
        return xml.contains(expr);
    }
}
