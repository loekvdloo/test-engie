package nl.engie.allocation.pipeline;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.enums.MessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context object that flows through the entire pipeline.
 * Carries the message, accumulated errors, and shared state between steps.
 */
public class PipelineContext {

    private final MarketMessage message;
    private final Map<String, Object> attributes = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<ValidationError> validationErrors = new ArrayList<>();

    private boolean halted = false;
    private boolean technicallyValid = true;
    private boolean businessValid = true;
    private MessageType detectedMessageType;
    private int assignedPriority = 5;
    private boolean parked = false;
    private boolean nack = false;
    private String responseXml;
    private MessageHeaders messageHeaders = MessageHeaders.empty();

    public PipelineContext(MarketMessage message) {
        this.message = message;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void addValidationError(String code, String message) {
        validationErrors.add(new ValidationError(code, message));
        businessValid = false;
    }

    public boolean hasErrors() {
        return !errors.isEmpty() || !validationErrors.isEmpty();
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }

    public MarketMessage getMessage() { return message; }
    public Map<String, Object> getAttributes() { return attributes; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public List<ValidationError> getValidationErrors() { return validationErrors; }
    public boolean isHalted() { return halted; }
    public boolean isTechnicallyValid() { return technicallyValid; }
    public boolean isBusinessValid() { return businessValid; }
    public MessageType getDetectedMessageType() { return detectedMessageType; }
    public int getAssignedPriority() { return assignedPriority; }
    public boolean isParked() { return parked; }
    public boolean isNack() { return nack; }
    public String getResponseXml() { return responseXml; }
    public MessageHeaders getMessageHeaders() { return messageHeaders; }

    public void setHalted(boolean halted) { this.halted = halted; }
    public void setTechnicallyValid(boolean technicallyValid) { this.technicallyValid = technicallyValid; }
    public void setBusinessValid(boolean businessValid) { this.businessValid = businessValid; }
    public void setDetectedMessageType(MessageType detectedMessageType) { this.detectedMessageType = detectedMessageType; }
    public void setAssignedPriority(int assignedPriority) { this.assignedPriority = assignedPriority; }
    public void setParked(boolean parked) { this.parked = parked; }
    public void setNack(boolean nack) { this.nack = nack; }
    public void setResponseXml(String responseXml) { this.responseXml = responseXml; }
    public void setMessageHeaders(MessageHeaders messageHeaders) { this.messageHeaders = messageHeaders != null ? messageHeaders : MessageHeaders.empty(); }

    /**
     * Represents a validation error with code and message.
     */
    public record ValidationError(String code, String message) {}

    /**
     * Parsed message header fields used across validation and response generation.
     */
    public record MessageHeaders(String messageId,
                                 String processTypeId,
                                 String senderBusinessId,
                                 String receiverBusinessId,
                                 String senderSoapId,
                                 String receiverSoapId,
                                 String correlationIdBusiness,
                                 String correlationIdSoap,
                                 String contentType,
                                 String technicalMessageId,
                                 String createdDateTime) {
        public static MessageHeaders empty() {
            return new MessageHeaders(null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}
