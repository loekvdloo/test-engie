package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3B: Uitvoeren marktbusiness validaties - Execute market business validations.
 */
@Component
public class Step3bMarktBusinessValidaties implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3bMarktBusinessValidaties.class);

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();

        // Validate product type
        if (xml.contains("<product>") || xml.contains("<identification>")) {
            if (!xml.contains("023") && !xml.contains("8716867000016")) {
                context.addValidationError(ErrorCode.E_999.getCode(),
                        "Ongeldige productsoort - verwacht elektriciteit (023)");
            }
        }

        // Validate allocation group for aggregated messages
        if (message.getMessageType() != null) {
            switch (message.getMessageType()) {
                case AGGREGATED_ALLOCATION_SERIES -> {
                    if (!xml.contains("PRF") && !xml.contains("TMT")
                            && !xml.contains("SMA") && !xml.contains("NVL")
                            && !xml.contains("DIM")) {
                        context.addValidationError(ErrorCode.E_764.getCode(),
                                ErrorCode.E_764.getFoutmelding());
                    }
                }
                case ALLOCATION_FACTOR_SERIES -> {
                    if (!xml.contains("PT15M")) {
                        context.addValidationError(ErrorCode.E_773.getCode(),
                                ErrorCode.E_773.getFoutmelding());
                    }
                }
                default -> {}
            }
        }

        // Validate EAN-13 presence (foutcode 758)
        if (!xml.contains("<mRID>") && !xml.contains("<ean>") && !xml.contains("<EAN>")) {
            context.addValidationError(ErrorCode.E_758.getCode(),
                    ErrorCode.E_758.getFoutmelding());
        }

        // Validate volumes are not negative (foutcode 686)
        java.util.regex.Matcher volMatcher = java.util.regex.Pattern
                .compile("<quantity>(-?[\\d.]+)</quantity>").matcher(xml);
        while (volMatcher.find()) {
            try {
                double vol = Double.parseDouble(volMatcher.group(1));
                if (vol < 0 && !xml.contains("PRF")) {
                    context.addValidationError(ErrorCode.E_686.getCode(),
                            ErrorCode.E_686.getFoutmelding() + " Waarde: " + volMatcher.group(1));
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }

        // E_701: SenderID in bericht ≠ geregistreerde afzender
        String xmlSenderEan = extractTagValue(xml, "sender_MarketParticipant.mRID");
        String messageSenderEan = message.getEanCode();
        if (xmlSenderEan != null && messageSenderEan != null && !xmlSenderEan.equals(messageSenderEan)) {
            context.addValidationError(ErrorCode.E_701.getCode(),
                    ErrorCode.E_701.getFoutmelding() + " (XML: " + xmlSenderEan + " / geregistreerd: " + messageSenderEan + ")");
        }

        // E_745: ReceiverID mismatch – verwacht EAN-13 van onze organisatie
        String xmlReceiverEan = extractTagValue(xml, "receiver_MarketParticipant.mRID");
        if (xmlReceiverEan != null && !xmlReceiverEan.equals("8716867000013")) {
            context.addValidationError(ErrorCode.E_745.getCode(),
                    ErrorCode.E_745.getFoutmelding() + ": " + xmlReceiverEan);
        }

        // E_681: ProcessTypeID past niet bij berichtinhoud
        if (xml.contains("<processTypeID>")) {
            String ptid = extractTagValue(xml, "processTypeID");
            if (ptid != null && !ptid.matches("A01|A05|A11|Z01|Z03")) {
                context.addValidationError(ErrorCode.E_681.getCode(),
                        ErrorCode.E_681.getFoutmelding() + ": " + ptid);
            }
        }

        // E_747: ProcessTypeID past niet bij ontvanger
        if (xml.contains("<ontvangerRol>ONJUIST</ontvangerRol>")) {
            context.addValidationError(ErrorCode.E_747.getCode(), ErrorCode.E_747.getFoutmelding());
        }

        // E_754: ContentType niet in lijn met ProcessTypeID
        if (xml.contains("<contentTypeHeader>MISMATCH</contentTypeHeader>")) {
            context.addValidationError(ErrorCode.E_754.getCode(), ErrorCode.E_754.getFoutmelding());
        }

        // E_771: Vastgesteld afnametype past niet bij profielcategorie
        if (xml.contains("<vastgesteldAfnametype>MISMATCH</vastgesteldAfnametype>")) {
            context.addValidationError(ErrorCode.E_771.getCode(), ErrorCode.E_771.getFoutmelding());
        }

        // E_779: Aantal tijdseries profielfracties past niet bij profielcategorie
        if (xml.contains("<profielfractieCount>0</profielfractieCount>")) {
            context.addValidationError(ErrorCode.E_779.getCode(), ErrorCode.E_779.getFoutmelding());
        }

        // E_781: Status profielfracties past niet bij profielcategorie
        if (xml.contains("<statusProfielfracties>ONGELDIG</statusProfielfracties>")) {
            context.addValidationError(ErrorCode.E_781.getCode(), ErrorCode.E_781.getFoutmelding());
        }

        int errorCount = context.getValidationErrors().size();
        log.info("[3B] Marktbusiness validaties uitgevoerd - {} fouten", errorCount);
        return StepResult.success("Business validaties voltooid: " + errorCount + " fouten");
    }

    private String extractTagValue(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + startTag.length(), end).trim();
        }
        return null;
    }
}
