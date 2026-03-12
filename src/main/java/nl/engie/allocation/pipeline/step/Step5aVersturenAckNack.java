package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.MarketResponse;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineStep;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.MarketResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Step 5A: Versturen ACK/NACK richting markt - Send ACK/NACK to market.
 */
@Component
public class Step5aVersturenAckNack implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(Step5aVersturenAckNack.class);

    private final MarketResponseRepository responseRepository;

    public Step5aVersturenAckNack(MarketResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    @Override
    public StepCode getStepCode() {
        return StepCode.STEP_5A;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        var message = context.getMessage();

        var responseOpt = responseRepository.findByMarketMessageId(message.getId());
        if (responseOpt.isPresent()) {
            MarketResponse response = responseOpt.get();
            response.setSentAt(LocalDateTime.now());
            responseRepository.save(response);

            message.setStatus(MessageStatus.RESPONSE_SENT);

            log.info("[5A] {} verstuurd richting markt: {}",
                    response.getResponseType(), response.getResponseUuid());
            return StepResult.success(response.getResponseType() + " verstuurd naar markt");
        }

        log.warn("[5A] Geen respons gevonden om te versturen");
        return StepResult.failure("Geen respons beschikbaar om te versturen");
    }
}
