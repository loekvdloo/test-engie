package nl.engie.allocation.service;

import nl.engie.allocation.dto.MessageStatusResponse;
import nl.engie.allocation.dto.MessageSubmitRequest;
import nl.engie.allocation.dto.StepStatusDto;
import nl.engie.allocation.dto.ValidationErrorDto;
import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.ProcessingStep;
import nl.engie.allocation.model.entity.ValidationResult;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.PipelineOrchestrator;
import nl.engie.allocation.repository.MarketMessageRepository;
import nl.engie.allocation.repository.MarketResponseRepository;
import nl.engie.allocation.repository.ProcessingStepRepository;
import nl.engie.allocation.repository.ValidationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MarketMessageService {

    private static final Logger log = LoggerFactory.getLogger(MarketMessageService.class);

    private final MarketMessageRepository messageRepository;
    private final ProcessingStepRepository stepRepository;
    private final MarketResponseRepository responseRepository;
    private final ValidationResultRepository validationResultRepository;
    private final PipelineOrchestrator pipelineOrchestrator;

    public MarketMessageService(MarketMessageRepository messageRepository,
                                ProcessingStepRepository stepRepository,
                                MarketResponseRepository responseRepository,
                                ValidationResultRepository validationResultRepository,
                                PipelineOrchestrator pipelineOrchestrator) {
        this.messageRepository = messageRepository;
        this.stepRepository = stepRepository;
        this.responseRepository = responseRepository;
        this.validationResultRepository = validationResultRepository;
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    /**
     * Submit a new market message for processing.
     * Returns immediately with a message UUID, then processes asynchronously.
     */
    public String submitMessage(MessageSubmitRequest request) {
        String uuid = UUID.randomUUID().toString();

        MarketMessage message = MarketMessage.builder()
                .messageUuid(uuid)
                .xmlContent(request.getXmlContent())
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .isManualEntry(request.isManualEntry())
                .eanCode(request.getEanCode())
                .build();

        message = messageRepository.save(message);

        // Initialize all pipeline steps
        pipelineOrchestrator.initializeSteps(message);

        // Process the message through the pipeline
        processMessage(message.getId());

        return uuid;
    }

    /**
     * Process a message through the complete pipeline.
     */
    public void processMessage(Long messageId) {
        MarketMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Bericht niet gevonden: " + messageId));

        log.info("Starting pipeline processing for message: {}", message.getMessageUuid());
        PipelineContext context = pipelineOrchestrator.executePipeline(message);
        log.info("Pipeline processing completed for message: {} - status: {}",
                message.getMessageUuid(), message.getStatus());
    }

    /**
     * Get the current status of a message and its pipeline steps.
     */
    @Transactional(readOnly = true)
    public MessageStatusResponse getMessageStatus(String messageUuid) {
        MarketMessage message = messageRepository.findByMessageUuid(messageUuid)
                .orElseThrow(() -> new RuntimeException("Bericht niet gevonden: " + messageUuid));

        List<ProcessingStep> steps = stepRepository
                .findByMarketMessageIdOrderByStepOrderAsc(message.getId());

        List<StepStatusDto> stepDtos = steps.stream()
                .map(s -> new StepStatusDto(
                        s.getStepCode().name(),
                        s.getStepName(),
                        s.getPhaseName(),
                        s.getStepOrder(),
                        s.getStatus().name(),
                        s.getStartedAt(),
                        s.getCompletedAt(),
                        s.getResultMessage(),
                        s.getErrorMessage()))
                .toList();

        String responseXml = null;
        String responseType = null;
        var responseOpt = responseRepository.findByMarketMessageId(message.getId());
        if (responseOpt.isPresent()) {
            responseXml = responseOpt.get().getXmlResponse();
            responseType = responseOpt.get().getResponseType().name();
        }

        // Collect validation error codes for NACK responses
        List<ValidationErrorDto> errorDtos = validationResultRepository
                .findByMarketMessageIdAndIsValidFalse(message.getId())
                .stream()
                .map(v -> new ValidationErrorDto(
                        v.getErrorCode(),
                        v.getErrorMessage(),
                        v.getRuleCode()))
                .toList();

        return new MessageStatusResponse(
                message.getMessageUuid(),
                message.getMessageType() != null ? message.getMessageType().name() : null,
                message.getStatus().name(),
                message.getCurrentStep() != null ? message.getCurrentStep().name() : null,
                message.getReceivedAt(),
                message.getCompletedAt(),
                message.getPriority(),
                responseType,
                responseXml,
                stepDtos,
                errorDtos);
    }

    /**
     * Get all messages with a specific status.
     */
    @Transactional(readOnly = true)
    public List<MessageStatusResponse> getMessagesByStatus(MessageStatus status) {
        return messageRepository.findByStatus(status).stream()
                .map(m -> getMessageStatus(m.getMessageUuid()))
                .toList();
    }

    /**
     * Get all messages with steps and response type.
     */
    @Transactional(readOnly = true)
    public List<MessageStatusResponse> getAllMessages() {
        return messageRepository.findAll().stream()
                .map(m -> getMessageStatus(m.getMessageUuid()))
                .toList();
    }

    /**
     * Reprocess a failed or parked message.
     */
    @Transactional
    public String reprocessMessage(String messageUuid) {
        MarketMessage message = messageRepository.findByMessageUuid(messageUuid)
                .orElseThrow(() -> new RuntimeException("Bericht niet gevonden: " + messageUuid));

        if (message.getStatus() != MessageStatus.FAILED
                && message.getStatus() != MessageStatus.PARKED) {
            throw new RuntimeException("Bericht kan alleen herverwerkt worden als status FAILED of PARKED is");
        }

        // Reset message status
        message.setStatus(MessageStatus.RECEIVED);
        message.setCurrentStep(null);
        message.setCompletedAt(null);
        messageRepository.save(message);

        // Delete old processing steps
        List<ProcessingStep> oldSteps = stepRepository
                .findByMarketMessageIdOrderByStepOrderAsc(message.getId());
        stepRepository.deleteAll(oldSteps);

        // Re-initialize and reprocess
        pipelineOrchestrator.initializeSteps(message);
        processMessage(message.getId());

        return message.getMessageUuid();
    }
}
