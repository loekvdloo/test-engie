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
import java.util.UUID;

/**
 * Step 4A: Genereren ACK bij succes - Generate acknowledgement on success.
 */
@Component
public class Step4aGenereerAck implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step4aGenereerAck.class);

    private final MarketResponseRepository responseRepository;

    public Step4aGenereerAck(MarketResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_4A;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        if (context.hasErrors() || !context.isBusinessValid()) {
            return StepResult.skipped("Fouten aanwezig - geen ACK gegenereerd");
        }

        var message = context.getMessage();
        String responseUuid = UUID.randomUUID().toString();

        String receivedMessageId = message.getExternalMessageId() != null
                ? message.getExternalMessageId()
                : message.getMessageUuid();
        String correlationId = preferredCorrelationId(context);

        String ackXml = generateAckXml(responseUuid, receivedMessageId, correlationId, message.getMessageType());

        MarketResponse response = MarketResponse.builder()
                .marketMessage(message)
                .responseUuid(responseUuid)
                .responseType(ResponseType.ACK)
                .xmlResponse(ackXml)
                .build();
        responseRepository.save(response);

        message.setStatus(MessageStatus.ACK_GENERATED);
        context.setResponseXml(ackXml);
        context.setNack(false);

        log.info("[4A] ACK gegenereerd: {}", responseUuid);
        return StepResult.success("ACK gegenereerd: " + responseUuid);
    }

    private String generateAckXml(String responseUuid, String receivedMessageId,
                                  String correlationId,
                                  nl.engie.allocation.model.enums.MessageType messageType) {
        String root = acknowledgementRoot(messageType);
        String correlationXml = (correlationId == null || correlationId.isBlank())
                ? ""
                : "    <correlationID>" + correlationId + "</correlationID>\n";
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <%s>
                    <mRID>%s</mRID>
                    <createdDateTime>%s</createdDateTime>
                %s    <Reason>
                        <code>000</code>
                    </Reason>
                    <Received_MarketDocument>
                        <mRID>%s</mRID>
                    </Received_MarketDocument>
                </%s>
                """.formatted(root,
                responseUuid,
                LocalDateTime.now().toString() + "Z",
                correlationXml,
                receivedMessageId,
                root);
    }

    private String preferredCorrelationId(PipelineContext context) {
        if (context.getMessageHeaders() == null) {
            return null;
        }
        if (context.getMessageHeaders().correlationIdSoap() != null
                && !context.getMessageHeaders().correlationIdSoap().isBlank()) {
            return context.getMessageHeaders().correlationIdSoap();
        }
        return context.getMessageHeaders().correlationIdBusiness();
    }

    private String acknowledgementRoot(nl.engie.allocation.model.enums.MessageType type) {
        if (type == null) {
            return "Acknowledgement_MarketDocument";
        }
        return switch (type) {
            case ALLOCATION_SERIES -> "AllocationSeriesAcknowledgement";
            case AGGREGATED_ALLOCATION_SERIES -> "AggregatedAllocationSeriesAcknowledgement";
            case ALLOCATION_FACTOR_SERIES -> "AllocationFactorSeriesAcknowledgement";
            default -> "Acknowledgement_MarketDocument";
        };
    }
}
