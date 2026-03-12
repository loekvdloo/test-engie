package nl.engie.allocation.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_logs")
public class ProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MarketMessage marketMessage;

    @Column(name = "step_code", nullable = false, length = 10)
    private String stepCode;

    @Column(name = "log_level", nullable = false, length = 10)
    private String logLevel;

    @Lob
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();

    public ProcessingLog() {
    }

    public ProcessingLog(Long id, MarketMessage marketMessage, String stepCode, String logLevel,
                         String message, LocalDateTime loggedAt) {
        this.id = id;
        this.marketMessage = marketMessage;
        this.stepCode = stepCode;
        this.logLevel = logLevel;
        this.message = message;
        this.loggedAt = loggedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketMessage getMarketMessage() { return marketMessage; }
    public void setMarketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; }
    public String getStepCode() { return stepCode; }
    public void setStepCode(String stepCode) { this.stepCode = stepCode; }
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }

    public static ProcessingLogBuilder builder() {
        return new ProcessingLogBuilder();
    }

    public static class ProcessingLogBuilder {
        private Long id;
        private MarketMessage marketMessage;
        private String stepCode;
        private String logLevel;
        private String message;
        private LocalDateTime loggedAt = LocalDateTime.now();

        ProcessingLogBuilder() {}

        public ProcessingLogBuilder id(Long id) { this.id = id; return this; }
        public ProcessingLogBuilder marketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; return this; }
        public ProcessingLogBuilder stepCode(String stepCode) { this.stepCode = stepCode; return this; }
        public ProcessingLogBuilder logLevel(String logLevel) { this.logLevel = logLevel; return this; }
        public ProcessingLogBuilder message(String message) { this.message = message; return this; }
        public ProcessingLogBuilder loggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; return this; }

        public ProcessingLog build() {
            return new ProcessingLog(id, marketMessage, stepCode, logLevel, message, loggedAt);
        }
    }
}
