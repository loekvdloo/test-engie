package nl.engie.allocation.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_results")
public class ValidationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MarketMessage marketMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private ValidationRule validationRule;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "validated_at", nullable = false)
    private LocalDateTime validatedAt = LocalDateTime.now();

    public ValidationResult() {
    }

    public ValidationResult(Long id, MarketMessage marketMessage, ValidationRule validationRule,
                            String ruleCode, Boolean isValid, String errorCode,
                            String errorMessage, LocalDateTime validatedAt) {
        this.id = id;
        this.marketMessage = marketMessage;
        this.validationRule = validationRule;
        this.ruleCode = ruleCode;
        this.isValid = isValid;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.validatedAt = validatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketMessage getMarketMessage() { return marketMessage; }
    public void setMarketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; }
    public ValidationRule getValidationRule() { return validationRule; }
    public void setValidationRule(ValidationRule validationRule) { this.validationRule = validationRule; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    public static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    public static class ValidationResultBuilder {
        private Long id;
        private MarketMessage marketMessage;
        private ValidationRule validationRule;
        private String ruleCode;
        private Boolean isValid;
        private String errorCode;
        private String errorMessage;
        private LocalDateTime validatedAt = LocalDateTime.now();

        ValidationResultBuilder() {}

        public ValidationResultBuilder id(Long id) { this.id = id; return this; }
        public ValidationResultBuilder marketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; return this; }
        public ValidationResultBuilder validationRule(ValidationRule validationRule) { this.validationRule = validationRule; return this; }
        public ValidationResultBuilder ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public ValidationResultBuilder isValid(Boolean isValid) { this.isValid = isValid; return this; }
        public ValidationResultBuilder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public ValidationResultBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public ValidationResultBuilder validatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; return this; }

        public ValidationResult build() {
            return new ValidationResult(id, marketMessage, validationRule, ruleCode, isValid,
                    errorCode, errorMessage, validatedAt);
        }
    }
}
