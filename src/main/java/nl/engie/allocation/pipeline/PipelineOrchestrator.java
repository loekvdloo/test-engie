package nl.engie.allocation.pipeline;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.ProcessingLog;
import nl.engie.allocation.model.entity.ProcessingStep;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.model.enums.StepStatus;
import nl.engie.allocation.repository.MarketMessageRepository;
import nl.engie.allocation.repository.ProcessingLogRepository;
import nl.engie.allocation.repository.ProcessingStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Orchestrates the pipeline execution.
 * Enforces strict sequential ordering: step 1A must complete before 1B, etc.
 * Auto-discovers all PipelineStep beans via Spring injection.
 */
@Component
public class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final Map<StepCode, PipelineStep> stepRegistry = new LinkedHashMap<>();
    private final MarketMessageRepository messageRepository;
    private final ProcessingStepRepository stepRepository;
    private final ProcessingLogRepository logRepository;

    public PipelineOrchestrator(MarketMessageRepository messageRepository,
                                 ProcessingStepRepository stepRepository,
                                 ProcessingLogRepository logRepository,
                                 List<PipelineStep> steps) {
        this.messageRepository = messageRepository;
        this.stepRepository = stepRepository;
        this.logRepository = logRepository;

        // Auto-register all PipelineStep beans, sorted by StepCode order
        steps.stream()
                .sorted(Comparator.comparingInt(s -> s.getStepCode().getOrder()))
                .forEach(step -> {
                    stepRegistry.put(step.getStepCode(), step);
                    log.debug("Registered pipeline step: {} - {}",
                            step.getStepCode(), step.getStepCode().getStepName());
                });
        log.info("Pipeline initialized with {} steps", stepRegistry.size());
    }

    /**
     * Initialize all processing step records for a message.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeSteps(MarketMessage message) {
        for (StepCode code : StepCode.values()) {
            ProcessingStep step = ProcessingStep.builder()
                    .marketMessage(message)
                    .stepCode(code)
                    .stepName(code.getStepName())
                    .phaseName(code.getPhaseName())
                    .stepOrder(code.getOrder())
                    .status(StepStatus.PENDING)
                    .build();
            stepRepository.save(step);
        }
    }

    /**
     * Execute the full pipeline for a message, strictly in order.
     * Each step must complete successfully (or be skipped) before the next step starts.
     * NOT @Transactional - each DB operation runs in its own transaction to avoid session corruption.
     */
    public PipelineContext executePipeline(MarketMessage message) {
        log.info("Starting pipeline for message: {}", message.getMessageUuid());

        PipelineContext context = new PipelineContext(message);

        message.setStatus(MessageStatus.PROCESSING);
        message.setCurrentStep(StepCode.STEP_1A);
        saveMessage(message);

        // Execute steps in strict order
        for (StepCode stepCode : StepCode.values()) {
            if (context.isHalted()) {
                // Mark remaining steps as skipped
                markRemainingStepsSkipped(message.getId(), stepCode);
                break;
            }

            PipelineStep step = stepRegistry.get(stepCode);
            if (step == null) {
                log.warn("No handler registered for step: {}. Skipping.", stepCode);
                updateStepStatus(message.getId(), stepCode, StepStatus.SKIPPED,
                        "No handler registered", null, null, null);
                continue;
            }

            // Update current step on message
            message.setCurrentStep(stepCode);
            saveMessage(message);

            // Mark step as in-progress
                String inputSnapshot = buildInputSnapshot(context, stepCode);
                updateStepStatus(message.getId(), stepCode, StepStatus.IN_PROGRESS, null, null,
                    inputSnapshot, null);
            logStep(message, stepCode, "INFO", "Starting step: " + stepCode.getStepName());

            try {
                StepResult result = step.execute(context);

                if (result.isSkipped()) {
                    String outputSnapshot = buildOutputSnapshot(context, stepCode, result);
                    updateStepStatus(message.getId(), stepCode, StepStatus.SKIPPED,
                        result.getMessage(), null, null, outputSnapshot);
                    logStep(message, stepCode, "INFO", "Step skipped: " + result.getMessage());
                } else if (result.isSuccess()) {
                    String outputSnapshot = buildOutputSnapshot(context, stepCode, result);
                    updateStepStatus(message.getId(), stepCode, StepStatus.COMPLETED,
                        result.getMessage(), null, null, outputSnapshot);
                    logStep(message, stepCode, "INFO", "Step completed: " + result.getMessage());
                } else {
                    String outputSnapshot = buildOutputSnapshot(context, stepCode, result);
                    updateStepStatus(message.getId(), stepCode, StepStatus.FAILED,
                        null, result.getMessage(), null, outputSnapshot);
                    logStep(message, stepCode, "ERROR", "Step failed: " + result.getMessage());

                    // Don't halt on failure in outcome/response phases - they handle errors
                    if (stepCode.getOrder() < StepCode.STEP_4A.getOrder()) {
                        context.setHalted(true);
                        context.addError("Pipeline halted at step " + stepCode + ": " + result.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Exception in step {}: {}", stepCode, e.getMessage(), e);
                try {
                    String outputSnapshot = buildExceptionOutputSnapshot(context, stepCode, e);
                    updateStepStatus(message.getId(), stepCode, StepStatus.FAILED,
                            null, e.getMessage(), null, outputSnapshot);
                    logStep(message, stepCode, "ERROR", "Exception: " + e.getMessage());
                } catch (Exception logEx) {
                    log.error("Failed to log step failure: {}", logEx.getMessage());
                }
                context.setHalted(true);
                context.addError("Exception at step " + stepCode + ": " + e.getMessage());
            }
        }

        // Update final message status
        if (context.isHalted() && !context.isNack()) {
            message.setStatus(MessageStatus.FAILED);
        } else {
            message.setStatus(MessageStatus.COMPLETED);
            message.setCompletedAt(LocalDateTime.now());
        }
        saveMessage(message);

        log.info("Pipeline completed for message: {} with status: {}",
                message.getMessageUuid(), message.getStatus());
        return context;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMessage(MarketMessage message) {
        messageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStepStatus(Long messageId, StepCode stepCode, StepStatus status,
                                   String resultMessage, String errorMessage,
                                   String inputSnapshot, String outputSnapshot) {
        stepRepository.findByMarketMessageIdAndStepCode(messageId, stepCode)
                .ifPresent(step -> {
                    step.setStatus(status);
                    if (status == StepStatus.IN_PROGRESS) {
                        step.setStartedAt(LocalDateTime.now());
                    }
                    if (status == StepStatus.COMPLETED || status == StepStatus.FAILED
                            || status == StepStatus.SKIPPED) {
                        step.setCompletedAt(LocalDateTime.now());
                    }
                    step.setResultMessage(resultMessage);
                    step.setErrorMessage(errorMessage);
                    if (inputSnapshot != null) {
                        step.setInputSnapshot(truncateSnapshot(inputSnapshot));
                    }
                    if (outputSnapshot != null) {
                        step.setOutputSnapshot(truncateSnapshot(outputSnapshot));
                    }
                    stepRepository.save(step);
                });
    }

    private void markRemainingStepsSkipped(Long messageId, StepCode fromStep) {
        for (StepCode code : StepCode.values()) {
            if (code.getOrder() >= fromStep.getOrder()) {
                updateStepStatus(messageId, code, StepStatus.SKIPPED,
                        "Pipeline halted before this step", null, null, null);
            }
        }
    }

    private String buildInputSnapshot(PipelineContext context, StepCode stepCode) {
        MarketMessage message = context.getMessage();
        int xmlLen = message.getXmlContent() != null ? message.getXmlContent().length() : 0;
        int validationCount = context.getValidationErrors() != null ? context.getValidationErrors().size() : 0;
        return "step=" + stepCode.name()
                + " | messageStatus=" + (message.getStatus() != null ? message.getStatus().name() : "-")
                + " | currentStep=" + (message.getCurrentStep() != null ? message.getCurrentStep().name() : "-")
                + " | validationErrors=" + validationCount
                + " | xmlLength=" + xmlLen;
    }

    private String buildOutputSnapshot(PipelineContext context, StepCode stepCode, StepResult result) {
        MarketMessage message = context.getMessage();
        int validationCount = context.getValidationErrors() != null ? context.getValidationErrors().size() : 0;
        int errorCount = context.getErrors() != null ? context.getErrors().size() : 0;
        String responseType = context.getResponseXml() == null ? "-" : (context.isNack() ? "NACK" : "ACK");
        return "step=" + stepCode.name()
                + " | result=" + (result.isSuccess() ? "SUCCESS" : (result.isSkipped() ? "SKIPPED" : "FAILED"))
                + " | message=" + (result.getMessage() != null ? result.getMessage() : "-")
                + " | isNack=" + context.isNack()
                + " | responseType=" + responseType
                + " | validationErrors=" + validationCount
                + " | pipelineErrors=" + errorCount
                + " | responseXmlLength=" + (context.getResponseXml() != null ? context.getResponseXml().length() : 0)
                + " | messageStatus=" + (message.getStatus() != null ? message.getStatus().name() : "-");
    }

    private String buildExceptionOutputSnapshot(PipelineContext context, StepCode stepCode, Exception exception) {
        return "step=" + stepCode.name()
                + " | result=EXCEPTION"
                + " | exceptionType=" + exception.getClass().getSimpleName()
                + " | exceptionMessage=" + (exception.getMessage() != null ? exception.getMessage() : "-")
                + " | isNack=" + context.isNack();
    }

    private String truncateSnapshot(String snapshot) {
        if (snapshot == null) return null;
        int max = 8000;
        return snapshot.length() <= max ? snapshot : snapshot.substring(0, max) + "...(truncated)";
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStep(MarketMessage message, StepCode stepCode, String level, String logMessage) {
        ProcessingLog processingLog = ProcessingLog.builder()
                .marketMessage(message)
                .stepCode(stepCode.name())
                .logLevel(level)
                .message(logMessage)
                .loggedAt(LocalDateTime.now())
                .build();
        logRepository.save(processingLog);
    }

    public boolean isFullyRegistered() {
        return stepRegistry.size() == StepCode.values().length;
    }

    public Set<StepCode> getRegisteredSteps() {
        return Collections.unmodifiableSet(stepRegistry.keySet());
    }
}
