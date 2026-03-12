package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

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

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            var doc = builder.parse(new InputSource(new StringReader(xml)));
            String rootElement = doc.getDocumentElement().getTagName();

            // Remove namespace prefix if present
            if (rootElement.contains(":")) {
                rootElement = rootElement.substring(rootElement.indexOf(":") + 1);
            }

            MessageType type = MessageType.fromXmlRoot(rootElement);
            if (type == null) {
                // Try to detect from content
                if (xml.contains("AllocationSeries") && !xml.contains("Aggregated") && !xml.contains("Factor")) {
                    type = MessageType.ALLOCATION_SERIES;
                } else if (xml.contains("AggregatedAllocation")) {
                    type = MessageType.AGGREGATED_ALLOCATION_SERIES;
                } else if (xml.contains("AllocationFactor")) {
                    type = MessageType.ALLOCATION_FACTOR_SERIES;
                }
            }

            if (type != null) {
                message.setMessageType(type);
                context.setDetectedMessageType(type);
                log.info("[1E] Berichttype geïdentificeerd: {}", type);
                return StepResult.success("Berichttype: " + type.getDescription());
            } else {
                log.warn("[1E] Onbekend berichttype voor root element: {}", rootElement);
                return StepResult.failure("Onbekend berichttype: " + rootElement);
            }
        } catch (Exception e) {
            log.error("[1E] Fout bij identificatie berichttype: {}", e.getMessage());
            return StepResult.failure("Fout bij identificatie: " + e.getMessage());
        }
    }
}
