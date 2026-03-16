package nl.engie.allocation.model.entity;

import jakarta.persistence.*;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.model.enums.StepStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_steps")
public class ProcessingStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MarketMessage marketMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_code", nullable = false, length = 10)
    private StepCode stepCode;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "phase_name", nullable = false, length = 50)
    private String phaseName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "input_snapshot", columnDefinition = "TEXT")
    private String inputSnapshot;

    @Column(name = "output_snapshot", columnDefinition = "TEXT")
    private String outputSnapshot;

    public ProcessingStep() {
    }

    public ProcessingStep(Long id, MarketMessage marketMessage, StepCode stepCode, String stepName,
                          String phaseName, Integer stepOrder, StepStatus status,
                          LocalDateTime startedAt, LocalDateTime completedAt,
                          String resultMessage, String errorMessage) {
        this.id = id;
        this.marketMessage = marketMessage;
        this.stepCode = stepCode;
        this.stepName = stepName;
        this.phaseName = phaseName;
        this.stepOrder = stepOrder;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.resultMessage = resultMessage;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketMessage getMarketMessage() { return marketMessage; }
    public void setMarketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; }
    public StepCode getStepCode() { return stepCode; }
    public void setStepCode(StepCode stepCode) { this.stepCode = stepCode; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public String getPhaseName() { return phaseName; }
    public void setPhaseName(String phaseName) { this.phaseName = phaseName; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public StepStatus getStatus() { return status; }
    public void setStatus(StepStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getInputSnapshot() { return inputSnapshot; }
    public void setInputSnapshot(String inputSnapshot) { this.inputSnapshot = inputSnapshot; }
    public String getOutputSnapshot() { return outputSnapshot; }
    public void setOutputSnapshot(String outputSnapshot) { this.outputSnapshot = outputSnapshot; }

    public static ProcessingStepBuilder builder() {
        return new ProcessingStepBuilder();
    }

    public static class ProcessingStepBuilder {
        private Long id;
        private MarketMessage marketMessage;
        private StepCode stepCode;
        private String stepName;
        private String phaseName;
        private Integer stepOrder;
        private StepStatus status = StepStatus.PENDING;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String resultMessage;
        private String errorMessage;
        private String inputSnapshot;
        private String outputSnapshot;

        ProcessingStepBuilder() {}

        public ProcessingStepBuilder id(Long id) { this.id = id; return this; }
        public ProcessingStepBuilder marketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; return this; }
        public ProcessingStepBuilder stepCode(StepCode stepCode) { this.stepCode = stepCode; return this; }
        public ProcessingStepBuilder stepName(String stepName) { this.stepName = stepName; return this; }
        public ProcessingStepBuilder phaseName(String phaseName) { this.phaseName = phaseName; return this; }
        public ProcessingStepBuilder stepOrder(Integer stepOrder) { this.stepOrder = stepOrder; return this; }
        public ProcessingStepBuilder status(StepStatus status) { this.status = status; return this; }
        public ProcessingStepBuilder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public ProcessingStepBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public ProcessingStepBuilder resultMessage(String resultMessage) { this.resultMessage = resultMessage; return this; }
        public ProcessingStepBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public ProcessingStepBuilder inputSnapshot(String inputSnapshot) { this.inputSnapshot = inputSnapshot; return this; }
        public ProcessingStepBuilder outputSnapshot(String outputSnapshot) { this.outputSnapshot = outputSnapshot; return this; }

        public ProcessingStep build() {
            ProcessingStep step = new ProcessingStep(id, marketMessage, stepCode, stepName, phaseName, stepOrder,
                    status, startedAt, completedAt, resultMessage, errorMessage);
            step.setInputSnapshot(inputSnapshot);
            step.setOutputSnapshot(outputSnapshot);
            return step;
        }
    }
}
