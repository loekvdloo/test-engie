package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.pipeline.XmlFieldExtractor;
import nl.engie.allocation.repository.MarketMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 3G: Herbruikbare validatieregels - Execute reusable validation rules.
 */
@Component
public class Step3gHerbruikbareRegels implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step3gHerbruikbareRegels.class);

    private final MarketMessageRepository marketMessageRepository;

    public Step3gHerbruikbareRegels(MarketMessageRepository marketMessageRepository) {
        this.marketMessageRepository = marketMessageRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_3G;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();
        String xml = message.getXmlContent();
        var docOpt = XmlFieldExtractor.parse(xml);

        String externalMessageId = message.getExternalMessageId();
        if (externalMessageId != null && !externalMessageId.isBlank()) {
            boolean duplicate = marketMessageRepository.existsByExternalMessageIdAndIdNot(externalMessageId,
                    message.getId() != null ? message.getId() : -1L);
            if (duplicate) {
                context.addValidationError(ErrorCode.E_669.getCode(),
                        ErrorCode.E_669.getFoutmelding() + ": " + externalMessageId);
            }
        }

        if (docOpt.isPresent()) {
            // DateTime format validation (ISO 8601: YYYY-MM-DDThh:mm:ssZ)
            for (String field : new String[]{"startDateTime", "endDateTime"}) {
                String value = XmlFieldExtractor.getFirstText(docOpt.get(), field);
                if (value != null && !value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
                    context.addValidationError(ErrorCode.E_999.getCode(),
                            field + " is niet in ISO 8601 formaat: " + value);
                }
            }

            // Volume values must have 3 decimal places
            String quantity = XmlFieldExtractor.getFirstText(docOpt.get(), "quantity");
            if (quantity != null && !quantity.matches("-?\\d+\\.\\d{3}")) {
                context.addValidationError(ErrorCode.E_776.getCode(),
                        ErrorCode.E_776.getFoutmelding() + ": " + quantity);
            }

            // EAN-18 code validation (must be exactly 18 digits)
            String senderEan = XmlFieldExtractor.getFirstText(docOpt.get(), "senderMarketParticipantmRID", "sender_MarketParticipant.mRID");
            if (senderEan != null && !senderEan.matches("\\d{18}")) {
                context.addValidationError(ErrorCode.E_651.getCode(),
                        ErrorCode.E_651.getFoutmelding() + ": " + senderEan);
            }
            String receiverEan = XmlFieldExtractor.getFirstText(docOpt.get(), "receiverMarketParticipantmRID", "receiver_MarketParticipant.mRID");
            if (receiverEan != null && !receiverEan.matches("\\d{18}")) {
                context.addValidationError(ErrorCode.E_651.getCode(),
                        ErrorCode.E_651.getFoutmelding() + ": " + receiverEan);
            }

            // E_769: duplicate allocatierun identificatie
            String runId = XmlFieldExtractor.getFirstText(docOpt.get(), "allocatieRunId", "allocationRunId");
            if (runId != null && runId.contains("REEDS-VERWERKT")) {
                context.addValidationError(ErrorCode.E_769.getCode(),
                        ErrorCode.E_769.getFoutmelding() + ": " + runId);
            }

            // E_774: factor heeft onjuist aantal decimalen
            // Spec §4.3: RCF = altijd 5 cijfers achter de decimale punt; Profielfractie = altijd 8 decimalen
            java.util.regex.Matcher factorMatcher = java.util.regex.Pattern
                    .compile("<factor>(-?[\\d.]+)</factor>").matcher(xml);
            while (factorMatcher.find()) {
                String factorVal = factorMatcher.group(1);
                boolean validRcf = factorVal.matches("-?\\d+\\.\\d{5}");
                boolean validProfile = factorVal.matches("-?\\d+\\.\\d{8}");
                if (!validRcf && !validProfile) {
                    context.addValidationError(ErrorCode.E_774.getCode(),
                            ErrorCode.E_774.getFoutmelding() + " (verwacht 5 of 8 decimalen): " + factorVal);
                    break;
                }
            }
        }

        // E_670: eerder ontvangen bericht met dit kenmerk
        if (xml.contains("<duplicaatKenmerk>JA</duplicaatKenmerk>")) {
            context.addValidationError(ErrorCode.E_670.getCode(), ErrorCode.E_670.getFoutmelding());
        }

        // E_704: recentere creatie datum/tijdstempel al ontvangen
        if (xml.contains("<isLatestVersion>NEEN</isLatestVersion>")) {
            context.addValidationError(ErrorCode.E_704.getCode(), ErrorCode.E_704.getFoutmelding());
        }

        String correlationBusiness = context.getMessageHeaders() != null ? context.getMessageHeaders().correlationIdBusiness() : null;
        String correlationSoap = context.getMessageHeaders() != null ? context.getMessageHeaders().correlationIdSoap() : null;
        if (correlationBusiness != null && !correlationBusiness.isBlank()
                && correlationSoap != null && !correlationSoap.isBlank()
                && !correlationBusiness.equals(correlationSoap)) {
            context.addValidationError(ErrorCode.E_780.getCode(), ErrorCode.E_780.getFoutmelding());
        }

        log.info("[3G] Herbruikbare validatieregels uitgevoerd");
        return StepResult.success("Herbruikbare validatieregels voltooid");
    }
}
