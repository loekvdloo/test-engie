package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
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
                context.addValidationError(ErrorCode.E_765.getCode(),
                        ErrorCode.E_765.getFoutmelding() + " EAN: " + eanCode);
                log.warn("[3A] EAN {} niet gevonden in BRP register (foutcode {})", eanCode, ErrorCode.E_765.getCode());
                return StepResult.success("BRP register controle: EAN niet gevonden (" + ErrorCode.E_765.getCode() + ")");
            }
            log.info("[3A] EAN {} gevonden in BRP register", eanCode);
        } else {
            log.info("[3A] Geen EAN code beschikbaar voor BRP controle");
        }

        // E_761: BRP niet actief als BRP-er in het netgebied
        if (message.getXmlContent() != null && message.getXmlContent().contains("<brpActief>NEE</brpActief>")) {
            context.addValidationError(ErrorCode.E_761.getCode(), ErrorCode.E_761.getFoutmelding());
            log.warn("[3A] BRP is niet actief in netgebied (foutcode {})", ErrorCode.E_761.getCode());
        }

        // E_777: netgebied of allocatiepunt niet in LNB administratie
        if (message.getXmlContent() != null && message.getXmlContent().contains("<netgebiedEAN>")) {
            String xml = message.getXmlContent();
            int s = xml.indexOf("<netgebiedEAN>") + "<netgebiedEAN>".length();
            int e = xml.indexOf("</netgebiedEAN>");
            if (s > 0 && e > s) {
                String netEan = xml.substring(s, e).trim();
                if (!netEan.startsWith("871686700")) {
                    context.addValidationError(ErrorCode.E_777.getCode(),
                            ErrorCode.E_777.getFoutmelding() + ": " + netEan);
                    log.warn("[3A] Netgebied EAN onbekend in LNB administratie: {} (foutcode {})", netEan, ErrorCode.E_777.getCode());
                }
            }
        }

        return StepResult.success("BRP register controle voltooid");
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
