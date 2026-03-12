package nl.engie.allocation.pipeline.step;

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
 * Step 1C: Technische validatie - Validate XML structure.
 */
@Component
public class Step1cTechnischeValidatie implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step1cTechnischeValidatie.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_1C;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(xml)));

            context.setTechnicallyValid(true);
            log.info("[1C] Technische validatie geslaagd");
            return StepResult.success("XML technisch valide");
        } catch (Exception e) {
            context.setTechnicallyValid(false);
            context.addError("Technische validatie mislukt: " + e.getMessage());
            log.warn("[1C] Technische validatie mislukt: {}", e.getMessage());
            return StepResult.failure("XML niet valide: " + e.getMessage());
        }
    }
}
