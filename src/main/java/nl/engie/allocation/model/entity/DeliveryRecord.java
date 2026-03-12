package nl.engie.allocation.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_records")
public class DeliveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MarketMessage marketMessage;

    @Column(name = "delivery_target", nullable = false, length = 50)
    private String deliveryTarget = "RAW_LAYER";

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus = "PENDING";

    @Column(name = "raw_layer_path")
    private String rawLayerPath;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public DeliveryRecord() {
    }

    public DeliveryRecord(Long id, MarketMessage marketMessage, String deliveryTarget,
                          String deliveryStatus, String rawLayerPath, LocalDateTime deliveredAt,
                          LocalDateTime createdAt) {
        this.id = id;
        this.marketMessage = marketMessage;
        this.deliveryTarget = deliveryTarget;
        this.deliveryStatus = deliveryStatus;
        this.rawLayerPath = rawLayerPath;
        this.deliveredAt = deliveredAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MarketMessage getMarketMessage() { return marketMessage; }
    public void setMarketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; }
    public String getDeliveryTarget() { return deliveryTarget; }
    public void setDeliveryTarget(String deliveryTarget) { this.deliveryTarget = deliveryTarget; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getRawLayerPath() { return rawLayerPath; }
    public void setRawLayerPath(String rawLayerPath) { this.rawLayerPath = rawLayerPath; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static DeliveryRecordBuilder builder() {
        return new DeliveryRecordBuilder();
    }

    public static class DeliveryRecordBuilder {
        private Long id;
        private MarketMessage marketMessage;
        private String deliveryTarget = "RAW_LAYER";
        private String deliveryStatus = "PENDING";
        private String rawLayerPath;
        private LocalDateTime deliveredAt;
        private LocalDateTime createdAt;

        DeliveryRecordBuilder() {}

        public DeliveryRecordBuilder id(Long id) { this.id = id; return this; }
        public DeliveryRecordBuilder marketMessage(MarketMessage marketMessage) { this.marketMessage = marketMessage; return this; }
        public DeliveryRecordBuilder deliveryTarget(String deliveryTarget) { this.deliveryTarget = deliveryTarget; return this; }
        public DeliveryRecordBuilder deliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; return this; }
        public DeliveryRecordBuilder rawLayerPath(String rawLayerPath) { this.rawLayerPath = rawLayerPath; return this; }
        public DeliveryRecordBuilder deliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; return this; }
        public DeliveryRecordBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DeliveryRecord build() {
            return new DeliveryRecord(id, marketMessage, deliveryTarget, deliveryStatus,
                    rawLayerPath, deliveredAt, createdAt);
        }
    }
}
