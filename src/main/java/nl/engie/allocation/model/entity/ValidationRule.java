package nl.engie.allocation.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_rules")
public class ValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "rule_description", columnDefinition = "TEXT")
    private String ruleDescription;

    @Column(name = "message_type", length = 50)
    private String messageType;

    @Column(name = "rule_expression", nullable = false, columnDefinition = "TEXT")
    private String ruleExpression;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public ValidationRule() {
    }

    public ValidationRule(Long id, String ruleCode, String ruleName, String ruleDescription,
                          String messageType, String ruleExpression, String errorCode,
                          String errorMessage, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.messageType = messageType;
        this.ruleExpression = ruleExpression;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getRuleDescription() { return ruleDescription; }
    public void setRuleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getRuleExpression() { return ruleExpression; }
    public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static ValidationRuleBuilder builder() {
        return new ValidationRuleBuilder();
    }

    public static class ValidationRuleBuilder {
        private Long id;
        private String ruleCode;
        private String ruleName;
        private String ruleDescription;
        private String messageType;
        private String ruleExpression;
        private String errorCode;
        private String errorMessage;
        private Boolean isActive = true;
        private LocalDateTime createdAt;

        ValidationRuleBuilder() {}

        public ValidationRuleBuilder id(Long id) { this.id = id; return this; }
        public ValidationRuleBuilder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public ValidationRuleBuilder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public ValidationRuleBuilder ruleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; return this; }
        public ValidationRuleBuilder messageType(String messageType) { this.messageType = messageType; return this; }
        public ValidationRuleBuilder ruleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; return this; }
        public ValidationRuleBuilder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public ValidationRuleBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public ValidationRuleBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public ValidationRuleBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ValidationRule build() {
            return new ValidationRule(id, ruleCode, ruleName, ruleDescription, messageType,
                    ruleExpression, errorCode, errorMessage, isActive, createdAt);
        }
    }
}
