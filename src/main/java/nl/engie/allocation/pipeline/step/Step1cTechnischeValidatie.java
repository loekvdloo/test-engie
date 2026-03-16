package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.pipeline.XmlFieldExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.util.Map;

/**
 * Step 1C: Technische validatie - Validate XML structure + XSD and extract technical headers.
 */
@Component
public class Step1cTechnischeValidatie implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1cTechnischeValidatie.class);

    private static final Map<String, String> ROOT_SCHEMA_MAP = Map.of(
            "allocationseries", "xsd/allocation-series.xsd",
            "allocationseriesnotification", "xsd/allocation-series.xsd",
            "aggregatedallocation", "xsd/aggregated-allocation-series.xsd",
            "aggregatedallocationseriesnotification", "xsd/aggregated-allocation-series.xsd",
            "allocationfactorseries", "xsd/allocation-factor-series.xsd",
            "allocationfactorseriesnotification", "xsd/allocation-factor-series.xsd"
    );

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1C;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        Document document = XmlFieldExtractor.parse(xml).orElse(null);
        if (document == null) {
            context.setTechnicallyValid(false);
            context.addError("Technische validatie mislukt: XML niet leesbaar/well-formed");
            log.warn("[1C] XML niet leesbaar/well-formed");
            return StepResult.failure("XML niet valide: well-formedness");
        }

        populateHeaders(context, document);

        String root = XmlFieldExtractor.normalizeName(XmlFieldExtractor.getRootLocalName(document));
        String schemaPath = ROOT_SCHEMA_MAP.get(root);
        if (schemaPath != null) {
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema(new ClassPathResource(schemaPath).getURL());
                Validator validator = schema.newValidator();
                validator.validate(new DOMSource(document));
            } catch (Exception e) {
                context.setTechnicallyValid(false);
                context.addError("Technische validatie mislukt: XSD validatie gefaald - " + e.getMessage());
                log.warn("[1C] XSD validatie gefaald voor {}: {}", root, e.getMessage());
                return StepResult.failure("XML niet valide tegen XSD: " + e.getMessage());
            }
        } else {
            log.info("[1C] Geen XSD mapping voor root '{}', alleen well-formedness toegepast", root);
        }

        context.setTechnicallyValid(true);
        log.info("[1C] Technische validatie geslaagd");
        return StepResult.success("XML technisch valide");
    }

    private void populateHeaders(PipelineContext context, Document document) {
        String messageId = XmlFieldExtractor.getFirstText(document, "messageID", "messageId", "mRID");
        String processTypeId = XmlFieldExtractor.getFirstText(document, "processTypeID", "processTypeId");
        String senderBusinessId = XmlFieldExtractor.getFirstText(document,
                "senderMarketParticipantmRID", "sender_marketparticipantmrid", "senderID", "senderId");
        String receiverBusinessId = XmlFieldExtractor.getFirstText(document,
                "receiverMarketParticipantmRID", "receiver_marketparticipantmrid", "receiverID", "receiverId");
        String senderSoapId = XmlFieldExtractor.getFirstText(document, "soapSenderID", "soapSenderId");
        String receiverSoapId = XmlFieldExtractor.getFirstText(document, "soapReceiverID", "soapReceiverId");
        String correlationBusiness = XmlFieldExtractor.getFirstText(document,
                "correlationID", "businessCorrelationID", "businessCorrelationId");
        String correlationSoap = XmlFieldExtractor.getFirstText(document,
                "soapCorrelationID", "soapCorrelationId", "correlationIDSOAP");
        String contentType = XmlFieldExtractor.getFirstText(document, "contentType");
        String technicalMessageId = XmlFieldExtractor.getFirstText(document,
                "technicalMessageId", "technicalMessageID");
        String createdDateTime = XmlFieldExtractor.getFirstText(document,
                "createdDateTime", "creationDateTime", "created");

        context.setMessageHeaders(new PipelineContext.MessageHeaders(
                messageId,
                processTypeId,
                senderBusinessId,
                receiverBusinessId,
                senderSoapId,
                receiverSoapId,
                correlationBusiness,
                correlationSoap,
                contentType,
                technicalMessageId,
                createdDateTime
        ));
    }
}
