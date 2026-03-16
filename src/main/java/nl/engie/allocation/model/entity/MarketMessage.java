package nl.engie.allocation.model.entity;

import jakarta.persistence.*;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "market_messages")
public class MarketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_uuid", nullable = false, unique = true, length = 36)
    private String messageUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 50)
    private MessageType messageType;

    @Lob
    @Column(name = "xml_content", nullable = false, columnDefinition = "TEXT")
    private String xmlContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status = MessageStatus.RECEIVED;

    @Column(name = "priority")
    private Integer priority = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 10)
    private StepCode currentStep;

    @Column(name = "ean_code", length = 18)
    private String eanCode;

    @Column(name = "product_type", length = 10)
    private String productType;

    @Column(name = "allocation_group", length = 10)
    private String allocationGroup;

    @Column(name = "allocation_run_id", length = 36)
    private String allocationRunId;

    @Column(name = "external_message_id", length = 100)
    private String externalMessageId;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_manual_entry")
    private Boolean isManualEntry = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "marketMessage")
    @OrderBy("stepOrder ASC")
    private List<ProcessingStep> processingSteps = new ArrayList<>();

    @OneToMany(mappedBy = "marketMessage")
    private List<ProcessingLog> processingLogs = new ArrayList<>();

    @OneToMany(mappedBy = "marketMessage")
    private List<ValidationResult> validationResults = new ArrayList<>();

    @OneToOne(mappedBy = "marketMessage")
    private MarketResponse marketResponse;

    @OneToOne(mappedBy = "marketMessage")
    private DeliveryRecord deliveryRecord;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public MarketMessage() {
    }

    public MarketMessage(Long id, String messageUuid, MessageType messageType, String xmlContent,
                         MessageStatus status, Integer priority, StepCode currentStep,
                         String eanCode, String productType, String allocationGroup,
                         String allocationRunId, String externalMessageId,
                         LocalDateTime startDateTime, LocalDateTime endDateTime,
                         LocalDateTime receivedAt, LocalDateTime completedAt, Boolean isManualEntry,
                         LocalDateTime createdAt, LocalDateTime updatedAt,
                         List<ProcessingStep> processingSteps, List<ProcessingLog> processingLogs,
                         List<ValidationResult> validationResults, MarketResponse marketResponse,
                         DeliveryRecord deliveryRecord) {
        this.id = id;
        this.messageUuid = messageUuid;
        this.messageType = messageType;
        this.xmlContent = xmlContent;
        this.status = status;
        this.priority = priority;
        this.currentStep = currentStep;
        this.eanCode = eanCode;
        this.productType = productType;
        this.allocationGroup = allocationGroup;
        this.allocationRunId = allocationRunId;
        this.externalMessageId = externalMessageId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
        this.isManualEntry = isManualEntry;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.processingSteps = processingSteps;
        this.processingLogs = processingLogs;
        this.validationResults = validationResults;
        this.marketResponse = marketResponse;
        this.deliveryRecord = deliveryRecord;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageUuid() { return messageUuid; }
    public void setMessageUuid(String messageUuid) { this.messageUuid = messageUuid; }
    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
    public String getXmlContent() { return xmlContent; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public StepCode getCurrentStep() { return currentStep; }
    public void setCurrentStep(StepCode currentStep) { this.currentStep = currentStep; }
    public String getEanCode() { return eanCode; }
    public void setEanCode(String eanCode) { this.eanCode = eanCode; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getAllocationGroup() { return allocationGroup; }
    public void setAllocationGroup(String allocationGroup) { this.allocationGroup = allocationGroup; }
    public String getAllocationRunId() { return allocationRunId; }
    public void setAllocationRunId(String allocationRunId) { this.allocationRunId = allocationRunId; }
    public String getExternalMessageId() { return externalMessageId; }
    public void setExternalMessageId(String externalMessageId) { this.externalMessageId = externalMessageId; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Boolean getIsManualEntry() { return isManualEntry; }
    public void setIsManualEntry(Boolean isManualEntry) { this.isManualEntry = isManualEntry; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ProcessingStep> getProcessingSteps() { return processingSteps; }
    public void setProcessingSteps(List<ProcessingStep> processingSteps) { this.processingSteps = processingSteps; }
    public List<ProcessingLog> getProcessingLogs() { return processingLogs; }
    public void setProcessingLogs(List<ProcessingLog> processingLogs) { this.processingLogs = processingLogs; }
    public List<ValidationResult> getValidationResults() { return validationResults; }
    public void setValidationResults(List<ValidationResult> validationResults) { this.validationResults = validationResults; }
    public MarketResponse getMarketResponse() { return marketResponse; }
    public void setMarketResponse(MarketResponse marketResponse) { this.marketResponse = marketResponse; }
    public DeliveryRecord getDeliveryRecord() { return deliveryRecord; }
    public void setDeliveryRecord(DeliveryRecord deliveryRecord) { this.deliveryRecord = deliveryRecord; }

    public static MarketMessageBuilder builder() {
        return new MarketMessageBuilder();
    }

    public static class MarketMessageBuilder {
        private Long id;
        private String messageUuid;
        private MessageType messageType;
        private String xmlContent;
        private MessageStatus status = MessageStatus.RECEIVED;
        private Integer priority = 5;
        private StepCode currentStep;
        private String eanCode;
        private String productType;
        private String allocationGroup;
        private String allocationRunId;
        private String externalMessageId;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private LocalDateTime receivedAt;
        private LocalDateTime completedAt;
        private Boolean isManualEntry = false;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ProcessingStep> processingSteps = new ArrayList<>();
        private List<ProcessingLog> processingLogs = new ArrayList<>();
        private List<ValidationResult> validationResults = new ArrayList<>();
        private MarketResponse marketResponse;
        private DeliveryRecord deliveryRecord;

        MarketMessageBuilder() {}

        public MarketMessageBuilder id(Long id) { this.id = id; return this; }
        public MarketMessageBuilder messageUuid(String messageUuid) { this.messageUuid = messageUuid; return this; }
        public MarketMessageBuilder messageType(MessageType messageType) { this.messageType = messageType; return this; }
        public MarketMessageBuilder xmlContent(String xmlContent) { this.xmlContent = xmlContent; return this; }
        public MarketMessageBuilder status(MessageStatus status) { this.status = status; return this; }
        public MarketMessageBuilder priority(Integer priority) { this.priority = priority; return this; }
        public MarketMessageBuilder currentStep(StepCode currentStep) { this.currentStep = currentStep; return this; }
        public MarketMessageBuilder eanCode(String eanCode) { this.eanCode = eanCode; return this; }
        public MarketMessageBuilder productType(String productType) { this.productType = productType; return this; }
        public MarketMessageBuilder allocationGroup(String allocationGroup) { this.allocationGroup = allocationGroup; return this; }
        public MarketMessageBuilder allocationRunId(String allocationRunId) { this.allocationRunId = allocationRunId; return this; }
        public MarketMessageBuilder externalMessageId(String externalMessageId) { this.externalMessageId = externalMessageId; return this; }
        public MarketMessageBuilder startDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; return this; }
        public MarketMessageBuilder endDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; return this; }
        public MarketMessageBuilder receivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; return this; }
        public MarketMessageBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public MarketMessageBuilder isManualEntry(Boolean isManualEntry) { this.isManualEntry = isManualEntry; return this; }
        public MarketMessageBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public MarketMessageBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public MarketMessageBuilder processingSteps(List<ProcessingStep> processingSteps) { this.processingSteps = processingSteps; return this; }
        public MarketMessageBuilder processingLogs(List<ProcessingLog> processingLogs) { this.processingLogs = processingLogs; return this; }
        public MarketMessageBuilder validationResults(List<ValidationResult> validationResults) { this.validationResults = validationResults; return this; }
        public MarketMessageBuilder marketResponse(MarketResponse marketResponse) { this.marketResponse = marketResponse; return this; }
        public MarketMessageBuilder deliveryRecord(DeliveryRecord deliveryRecord) { this.deliveryRecord = deliveryRecord; return this; }

        public MarketMessage build() {
            return new MarketMessage(id, messageUuid, messageType, xmlContent, status, priority,
                    currentStep, eanCode, productType, allocationGroup, allocationRunId,
                    externalMessageId, startDateTime, endDateTime, receivedAt, completedAt, isManualEntry,
                    createdAt, updatedAt, processingSteps, processingLogs, validationResults,
                    marketResponse, deliveryRecord);
        }
    }
}
