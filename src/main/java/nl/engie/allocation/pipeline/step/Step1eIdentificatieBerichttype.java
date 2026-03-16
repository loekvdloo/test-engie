package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.pipeline.XmlFieldExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1E: Identificatie berichttype - Identify the message type from XML root element.
 */
@Component
public class Step1eIdentificatieBerichttype implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1eIdentificatieBerichttype.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1E;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        var docOpt = XmlFieldExtractor.parse(xml);
        if (docOpt.isEmpty()) {
            return StepResult.failure("Fout bij identificatie: XML niet parsebaar");
        }

        String rootElement = XmlFieldExtractor.getRootLocalName(docOpt.get());
        MessageType type = MessageType.fromXmlRoot(rootElement);
        if (type == null) {
            String normalizedRoot = XmlFieldExtractor.normalizeName(rootElement);
            if ("allocationseries".equals(normalizedRoot) || "allocationseriesnotification".equals(normalizedRoot)) {
                type = MessageType.ALLOCATION_SERIES;
            } else if ("aggregatedallocation".equals(normalizedRoot)
                    || "aggregatedallocationseriesnotification".equals(normalizedRoot)) {
                type = MessageType.AGGREGATED_ALLOCATION_SERIES;
            } else if ("allocationfactorseries".equals(normalizedRoot)
                    || "allocationfactorseriesnotification".equals(normalizedRoot)) {
                type = MessageType.ALLOCATION_FACTOR_SERIES;
            }
        }

        String externalMessageId = context.getMessageHeaders() != null ? context.getMessageHeaders().messageId() : null;
        if (externalMessageId == null || externalMessageId.isBlank()) {
            externalMessageId = XmlFieldExtractor.getFirstText(docOpt.get(), "messageID", "messageId", "mRID");
        }
        if (externalMessageId != null && !externalMessageId.isBlank()) {
            message.setExternalMessageId(externalMessageId);
        }

        if (type != null) {
            message.setMessageType(type);
            context.setDetectedMessageType(type);
            log.info("[1E] Berichttype geïdentificeerd: {}", type);
            return StepResult.success("Berichttype: " + type.getDescription());
        }

        log.warn("[1E] Onbekend berichttype voor root element: {}", rootElement);
        return StepResult.failure("Onbekend berichttype: " + rootElement);
    }
}

