package nl.engie.allocation.model.entity;

import jakarta.persistence.*;
import nl.engie.allocation.model.enums.ResponseType;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_responses")
public class MarketResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MarketMessage marketMessage;

    @Column(name = "response_uuid", nullable = false, unique = true, length = 36)
    private String responseUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false, length = 10)
    private ResponseType responseType;

    @Column(name = "error_codes", columnDefinition = "TEXT")
    private String errorCodes;

    @Column(name = "error_messages", columnDefinition = "TEXT")
    private String errorMessages;

    @Lob
    @Column(name = "xml_response", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String xmlResponse;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public MarketResponse() {
    }

    public MarketResponse(Long id, MarketMessage marketMessage, String responseUuid,
                          ResponseType responseType, String errorCodes, String errorMessages,
                          String xmlResponse, LocalDateTime sentAt, LocalDateTime createdAt) {
        this.id = id;
        this.marketMessage = marketMessage;
        this.responseUuid = responseUuid;
        this.responseType = responseType;
        this.errorCodes = errorCodes;
        this.errorMessages = errorMessages;
        this.xmlResponse = xmlResponse;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketMessage getMarketMessage() { return marketMessage; }
    public void setMarketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; }
    public String getResponseUuid() { return responseUuid; }
    public void setResponseUuid(String responseUuid) { this.responseUuid = responseUuid; }
    public ResponseType getResponseType() { return responseType; }
    public void setResponseType(ResponseType responseType) { this.responseType = responseType; }
    public String getErrorCodes() { return errorCodes; }
    public void setErrorCodes(String errorCodes) { this.errorCodes = errorCodes; }
    public String getErrorMessages() { return errorMessages; }
    public void setErrorMessages(String errorMessages) { this.errorMessages = errorMessages; }
    public String getXmlResponse() { return xmlResponse; }
    public void setXmlResponse(String xmlResponse) { this.xmlResponse = xmlResponse; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static MarketResponseBuilder builder() {
        return new MarketResponseBuilder();
    }

    public static class MarketResponseBuilder {
        private Long id;
        private MarketMessage marketMessage;
        private String responseUuid;
        private ResponseType responseType;
        private String errorCodes;
        private String errorMessages;
        private String xmlResponse;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;

        MarketResponseBuilder() {}

        public MarketResponseBuilder id(Long id) { this.id = id; return this; }
        public MarketResponseBuilder marketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; return this; }
        public MarketResponseBuilder responseUuid(String responseUuid) { this.responseUuid = responseUuid; return this; }
        public MarketResponseBuilder responseType(ResponseType responseType) { this.responseType = responseType; return this; }
        public MarketResponseBuilder errorCodes(String errorCodes) { this.errorCodes = errorCodes; return this; }
        public MarketResponseBuilder errorMessages(String errorMessages) { this.errorMessages = errorMessages; return this; }
        public MarketResponseBuilder xmlResponse(String xmlResponse) { this.xmlResponse = xmlResponse; return this; }
        public MarketResponseBuilder sentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
        public MarketResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public MarketResponse build() {
            return new MarketResponse(id, marketMessage, responseUuid, responseType, errorCodes,
                    errorMessages, xmlResponse, sentAt, createdAt);
        }
    }
}
