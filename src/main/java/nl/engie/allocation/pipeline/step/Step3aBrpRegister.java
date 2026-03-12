package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.BrpRegisterRepository;
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
 * Step 3A: Operational BRP register - Validate against BRP register.
 */
@Component
public class Step3aBrpRegister implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3aBrpRegister.class);

    private final BrpRegisterRepository brpRegisterRepository;

    public Step3aBrpRegister(BrpRegisterRepository brpRegisterRepository) {
        this.brpRegisterRepository = brpRegisterRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3A;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String eanCode = message.getEanCode();

        if (eanCode == null || eanCode.isBlank()) {
            try {
                eanCode = extractEanFromXml(message.getXmlContent());
                if (eanCode != null) {
                    message.setEanCode(eanCode);
                }
            } catch (Exception e) {
                log.warn("[3A] Kon EAN niet uit XML extraheren: {}", e.getMessage());
            }
        }

        if (eanCode != null && !eanCode.isBlank()) {
            boolean exists = brpRegisterRepository.existsByEanCodeAndIsActiveTrue(eanCode);
            if (!exists) {
                context.addValidationError("BRP001",
                        "EAN code " + eanCode + " niet gevonden in BRP register");
                log.warn("[3A] EAN {} niet gevonden in BRP register", eanCode);
                return StepResult.success("BRP register controle: EAN niet gevonden");
            }
            log.info("[3A] EAN {} gevonden in BRP register", eanCode);
            return StepResult.success("BRP register controle: EAN geldig");
        }

        log.info("[3A] Geen EAN code beschikbaar voor BRP controle");
        return StepResult.success("BRP register controle: geen EAN beschikbaar");
    }

    private String extractEanFromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        for (String tagName : new String[]{"mRID", "ean", "EAN", "eanCode"}) {
            NodeList nodes = doc.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                String value = nodes.item(0).getTextContent().trim();
                if (value.length() == 18 || value.length() == 13) {
                    return value;
                }
            }
        }
        return null;
    }
}
