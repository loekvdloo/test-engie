package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.MarketResponse;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.ResponseType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.MarketResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Step 4B: Genereren NACK bij fouten - Generate negative acknowledgement on errors.
 */
@Component
public class Step4bGenereerNack implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step4bGenereerNack.class);

    private final MarketResponseRepository responseRepository;

    public Step4bGenereerNack(MarketResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_4B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.hasErrors() && context.isBusinessValid()) {
            return StepResult.skipped("Geen fouten - NACK niet nodig");
        }

        var message = context.getMessage();
        String responseUuid = UUID.randomUUID().toString();

        String errorCodes = context.getValidationErrors().stream()
                .map(PipelineContext.ValidationError::code)
                .collect(Collectors.joining(","));
        String errorMessages = context.getValidationErrors().stream()
                .map(PipelineContext.ValidationError::message)
                .collect(Collectors.joining("; "));

        if (!context.getErrors().isEmpty()) {
            String pipelineErrors = String.join("; ", context.getErrors());
            errorMessages = errorMessages.isEmpty()
                    ? pipelineErrors : errorMessages + "; " + pipelineErrors;
            if (errorCodes.isEmpty()) errorCodes = "999";
        }

        String nackXml = generateNackXml(responseUuid, message.getMessageUuid(),
                context.getValidationErrors());

        MarketResponse response = MarketResponse.builder()
                .marketMessage(message)
                .responseUuid(responseUuid)
                .responseType(ResponseType.NACK)
                .errorCodes(errorCodes)
                .errorMessages(errorMessages)
                .xmlResponse(nackXml)
                .build();
        responseRepository.save(response);

        message.setStatus(MessageStatus.NACK_GENERATED);
        context.setResponseXml(nackXml);
        context.setNack(true);

        log.info("[4B] NACK gegenereerd: {} met fouten: {}", responseUuid, errorCodes);
        return StepResult.success("NACK gegenereerd: " + responseUuid);
    }

    private String generateNackXml(String responseUuid, String originalUuid,
                                    List<PipelineContext.ValidationError> errors) {
        StringBuilder reasons = new StringBuilder();
        for (var error : errors) {
            reasons.append("""
                        <Reason>
                            <code>%s</code>
                            <text>%s</text>
                        </Reason>
                    """.formatted(error.code(), escapeXml(error.message())));
        }
        if (reasons.isEmpty()) {
            reasons.append("""
                        <Reason>
                            <code>999</code>
                            <text>Onbekende fout</text>
                        </Reason>
                    """);
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Acknowledgement_MarketDocument>
                    <mRID>%s</mRID>
                    <createdDateTime>%s</createdDateTime>
                %s    <Received_MarketDocument>
                        <mRID>%s</mRID>
                    </Received_MarketDocument>
                </Acknowledgement_MarketDocument>
                """.formatted(responseUuid,
                LocalDateTime.now().toString() + "Z",
                reasons.toString(),
                originalUuid);
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
