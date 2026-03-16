package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.AllocationValidationSpec;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.pipeline.XmlFieldExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Step 3B: Uitvoeren marktbusiness validaties - Execute market business validations.
 */
@Component
public class Step3bMarktBusinessValidaties implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3bMarktBusinessValidaties.class);
    private static final Pattern DIGITS_13 = Pattern.compile("^\\d{13}$");

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3B;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();
        MessageType messageType = message.getMessageType() != null ? message.getMessageType() : context.getDetectedMessageType();

        var docOpt = XmlFieldExtractor.parse(xml);
        if (docOpt.isEmpty()) {
            context.addValidationError(ErrorCode.E_999.getCode(), "Business validatie overgeslagen: XML niet parsebaar");
            return StepResult.success("Business validaties voltooid: XML niet parsebaar");
        }

        var document = docOpt.get();
        PipelineContext.MessageHeaders headers = context.getMessageHeaders();

        validateProductAndUnit(context, document);
        validateAllocationGroupAndResolution(context, xml, messageType);
        validateEan13Rules(context, document, messageType);
        validateVolumeValues(context, xml);
        validateHeaderConsistency(context, headers, document, messageType, message);
        validateProfileSpecificMarkers(context, xml);

        int errorCount = context.getValidationErrors().size();
        log.info("[3B] Marktbusiness validaties uitgevoerd - {} fouten", errorCount);
        return StepResult.success("Business validaties voltooid: " + errorCount + " fouten");
    }

    private void validateProductAndUnit(PipelineContext context, org.w3c.dom.Document document) {
        List<String> productValues = XmlFieldExtractor.getAllTexts(document, "product", "identification", "productID", "productId");
        for (String value : productValues) {
            if (!AllocationValidationSpec.VALID_PRODUCT_CODES.contains(value)) {
                context.addValidationError(ErrorCode.E_667.getCode(), ErrorCode.E_667.getFoutmelding() + " Product=" + value);
                break;
            }
        }

        List<String> unitValues = XmlFieldExtractor.getAllTexts(document,
                "energyUnit", "unit", "quantityMeasureUnitname", "quantity_Measure_Unit.name");
        for (String unit : unitValues) {
            String normalized = unit.trim().toUpperCase();
            if (!normalized.isBlank() && !AllocationValidationSpec.VALID_ENERGY_UNITS.contains(normalized)) {
                context.addValidationError(ErrorCode.E_668.getCode(), ErrorCode.E_668.getFoutmelding() + " Unit=" + unit);
                break;
            }
        }

        String ean18 = XmlFieldExtractor.getFirstText(document, "pointmRID", "allocationpointmrid", "ean18", "mRID");
        if (ean18 != null && ean18.matches("\\d+") && ean18.length() == 18) {
            if (context.getMessage().getMessageType() == MessageType.ALLOCATION_SERIES) {
                if (!isLikelyValidEan18(ean18)) {
                    context.addValidationError(ErrorCode.E_650.getCode(), ErrorCode.E_650.getFoutmelding());
                }
            } else if (!isLikelyValidEan18(ean18)) {
                context.addValidationError(ErrorCode.E_651.getCode(), ErrorCode.E_651.getFoutmelding());
            }
        }

        if (XmlFieldExtractor.getFirstText(document, "originIndicator") != null
                && XmlFieldExtractor.getFirstText(document, "validationStatus") != null
                && XmlFieldExtractor.getFirstText(document, "repairMethod") != null) {
            String combination = String.join("|",
                    XmlFieldExtractor.getFirstText(document, "originIndicator"),
                    XmlFieldExtractor.getFirstText(document, "validationStatus"),
                    XmlFieldExtractor.getFirstText(document, "repairMethod"));
            if (combination.contains("ONGELDIG")) {
                context.addValidationError(ErrorCode.E_683.getCode(), ErrorCode.E_683.getFoutmelding());
            }
        }
    }

    private void validateAllocationGroupAndResolution(PipelineContext context, String xml, MessageType messageType) {
        if (messageType == MessageType.AGGREGATED_ALLOCATION_SERIES) {
            if (!xml.contains("PRF") && !xml.contains("TMT") && !xml.contains("SMA") && !xml.contains("NVL") && !xml.contains("DIM")) {
                context.addValidationError(ErrorCode.E_764.getCode(), ErrorCode.E_764.getFoutmelding());
            }
        }
        if (messageType == MessageType.ALLOCATION_FACTOR_SERIES && !xml.contains("PT15M")) {
            context.addValidationError(ErrorCode.E_773.getCode(), ErrorCode.E_773.getFoutmelding());
        }
    }

    private void validateEan13Rules(PipelineContext context, org.w3c.dom.Document document, MessageType messageType) {
        List<String> eanValues = XmlFieldExtractor.getAllTexts(document,
                "senderMarketParticipantmRID", "receiverMarketParticipantmRID", "ean", "brpean", "suppliermRID");
        long ean13Count = eanValues.stream().filter(v -> DIGITS_13.matcher(v).matches()).count();

        boolean hasRoleContext = !eanValues.isEmpty() || XmlFieldExtractor.getFirstText(document, "marketRole") != null;
        if (!hasRoleContext) {
            return;
        }

        if (messageType == MessageType.ALLOCATION_SERIES) {
            if (ean13Count == 0) {
                context.addValidationError(ErrorCode.E_759.getCode(), ErrorCode.E_759.getFoutmelding());
            }
        } else {
            if (ean13Count != 1) {
                context.addValidationError(ErrorCode.E_758.getCode(), ErrorCode.E_758.getFoutmelding());
            }
        }
    }

    private void validateVolumeValues(PipelineContext context, String xml) {
        java.util.regex.Matcher volMatcher = java.util.regex.Pattern
                .compile("<quantity>(-?[\\d.]+)</quantity>")
                .matcher(xml);
        while (volMatcher.find()) {
            try {
                double vol = Double.parseDouble(volMatcher.group(1));
                if (vol < 0 && !xml.contains("PRF")) {
                    context.addValidationError(ErrorCode.E_686.getCode(),
                            ErrorCode.E_686.getFoutmelding() + " Waarde: " + volMatcher.group(1));
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void validateHeaderConsistency(PipelineContext context,
                                           PipelineContext.MessageHeaders headers,
                                           org.w3c.dom.Document document,
                                           MessageType messageType,
                                           nl.engie.allocation.model.entity.MarketMessage message) {
        String senderBusiness = firstNonBlank(headers.senderBusinessId(),
                XmlFieldExtractor.getFirstText(document, "senderMarketParticipantmRID", "senderID"));
        String receiverBusiness = firstNonBlank(headers.receiverBusinessId(),
                XmlFieldExtractor.getFirstText(document, "receiverMarketParticipantmRID", "receiverID"));
        String senderSoap = firstNonBlank(headers.senderSoapId(), XmlFieldExtractor.getFirstText(document, "soapSenderID"));
        String receiverSoap = firstNonBlank(headers.receiverSoapId(), XmlFieldExtractor.getFirstText(document, "soapReceiverID"));

        if (senderSoap != null && senderBusiness != null && !senderSoap.equals(senderBusiness)) {
            context.addValidationError(ErrorCode.E_701.getCode(), ErrorCode.E_701.getFoutmelding());
        }
        if (receiverSoap != null && receiverBusiness != null && !receiverSoap.equals(receiverBusiness)) {
            context.addValidationError(ErrorCode.E_745.getCode(), ErrorCode.E_745.getFoutmelding());
        }

        if (senderBusiness != null && message.getEanCode() != null && !senderBusiness.equals(message.getEanCode())) {
            context.addValidationError(ErrorCode.E_701.getCode(),
                    ErrorCode.E_701.getFoutmelding() + " (XML: " + senderBusiness + " / geregistreerd: " + message.getEanCode() + ")");
        }

        String processTypeId = firstNonBlank(headers.processTypeId(), XmlFieldExtractor.getFirstText(document, "processTypeID"));
        if (processTypeId != null && !AllocationValidationSpec.isAllowedForMessageType(messageType, processTypeId)) {
            context.addValidationError(ErrorCode.E_681.getCode(), ErrorCode.E_681.getFoutmelding() + ": " + processTypeId);
        }

        String receiverRole = XmlFieldExtractor.getFirstText(document, "receiverRole", "ontvangerRol", "marketRole");
        if (processTypeId != null && receiverRole != null
                && !AllocationValidationSpec.isAllowedForReceiverRole(receiverRole, processTypeId)) {
            context.addValidationError(ErrorCode.E_747.getCode(), ErrorCode.E_747.getFoutmelding());
        }

        String contentType = firstNonBlank(headers.contentType(), XmlFieldExtractor.getFirstText(document, "contentType"));
        if (contentType != null && processTypeId != null && !isContentTypeInLineWithProcessType(contentType, processTypeId)) {
            context.addValidationError(ErrorCode.E_754.getCode(), ErrorCode.E_754.getFoutmelding());
        }
    }

    private void validateProfileSpecificMarkers(PipelineContext context, String xml) {
        if (xml.contains("<vastgesteldAfnametype>MISMATCH</vastgesteldAfnametype>")) {
            context.addValidationError(ErrorCode.E_771.getCode(), ErrorCode.E_771.getFoutmelding());
        }
        if (xml.contains("<profielfractieCount>0</profielfractieCount>")) {
            context.addValidationError(ErrorCode.E_779.getCode(), ErrorCode.E_779.getFoutmelding());
        }
        if (xml.contains("<statusProfielfracties>ONGELDIG</statusProfielfracties>")) {
            context.addValidationError(ErrorCode.E_781.getCode(), ErrorCode.E_781.getFoutmelding());
        }
    }

    private boolean isLikelyValidEan18(String value) {
        if (value == null || !value.matches("\\d{18}")) {
            return false;
        }
        int sum = 0;
        // GS1 check digit: positie van rechts, even posities *3
        for (int i = 0; i < 17; i++) {
            int digit = value.charAt(i) - '0';
            int positionFromRight = 17 - i;
            sum += (positionFromRight % 2 == 0) ? digit * 3 : digit;
        }
        int expectedCheckDigit = (10 - (sum % 10)) % 10;
        int actualCheckDigit = value.charAt(17) - '0';
        return expectedCheckDigit == actualCheckDigit;
    }

    private boolean isContentTypeInLineWithProcessType(String contentType, String processTypeId) {
        String c = contentType.toUpperCase();
        String p = processTypeId.toUpperCase();
        if (p.equals("N151")) {
            return c.contains("FACTOR") || c.contains("RCF") || c.contains("PROFILE");
        }
        if (p.equals("N101") || p.equals("N131") || p.equals("N141")) {
            return c.contains("ALLOCATIONSERIES") || c.contains("INDIVIDUAL");
        }
        return c.contains("AGGREGATED") || c.contains("ALLOCATION");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second != null && !second.isBlank()) ? second : null;
    }
}
