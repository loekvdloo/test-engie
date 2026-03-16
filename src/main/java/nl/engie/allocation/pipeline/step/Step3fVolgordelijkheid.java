package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Step 3F: Controle op volgordelijkheid - Check message sequence order.
 */
@Component
public class Step3fVolgordelijkheid implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3fVolgordelijkheid.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3F;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList positions = doc.getElementsByTagName("position");
            if (positions.getLength() > 0) {
                int expectedPosition = 1;
                for (int i = 0; i < positions.getLength(); i++) {
                    String posStr = positions.item(i).getTextContent().trim();
                    try {
                        int pos = Integer.parseInt(posStr);
                        if (i == 0 && pos != 1) {
                            context.addValidationError(ErrorCode.E_676.getCode(),
                                    ErrorCode.E_676.getFoutmelding() + " (gevonden: " + pos + ")");
                            break;
                        }
                        if (pos != expectedPosition) {
                            context.addValidationError(ErrorCode.E_782.getCode(),
                                    ErrorCode.E_782.getFoutmelding() + " bij positie " + pos
                                            + " (verwacht: " + expectedPosition + ")");
                            break;
                        }
                        expectedPosition++;
                    } catch (NumberFormatException e) {
                        context.addValidationError(ErrorCode.E_782.getCode(),
                                "Ongeldige positiewaarde: " + posStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[3F] Fout bij controle volgordelijkheid: {}", e.getMessage());
        }

        // E_671: verwacht aantal posities klopt niet met werkelijk gevonden posities
        if (xml.contains("<positionCount>")) {
            try {
                String xml2 = xml;
                int s = xml2.indexOf("<positionCount>") + "<positionCount>".length();
                int e2 = xml2.indexOf("</positionCount>");
                if (s > 0 && e2 > s) {
                    int expected = Integer.parseInt(xml2.substring(s, e2).trim());
                    DocumentBuilderFactory f2 = DocumentBuilderFactory.newInstance();
                    f2.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    Document d2 = f2.newDocumentBuilder().parse(new InputSource(new java.io.StringReader(xml2)));
                    int actual = d2.getElementsByTagName("position").getLength();
                    if (actual != expected) {
                        context.addValidationError(ErrorCode.E_671.getCode(),
                                ErrorCode.E_671.getFoutmelding() + ": verwacht=" + expected + " gevonden=" + actual);
                    }
                }
            } catch (Exception ex) {
                log.warn("[3F] Fout bij controle positionCount: {}", ex.getMessage());
            }
        }

        log.info("[3F] Controle op volgordelijkheid uitgevoerd");
        return StepResult.success("Volgordelijkheid gecontroleerd");
    }
}
