package nl.engie.allocation.model.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "brp_register")
public class BrpRegisterEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ean_code", nullable = false, unique = true, length = 18)
    private String eanCode;

    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    @Column(name = "market_role", nullable = false, length = 10)
    private String marketRole;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public BrpRegisterEntry() {
    }

    public BrpRegisterEntry(Long id, String eanCode, String partyName, String marketRole,
                            Boolean isActive, LocalDate validFrom, LocalDate validTo,
                            LocalDateTime createdAt) {
        this.id = id;
        this.eanCode = eanCode;
        this.partyName = partyName;
        this.marketRole = marketRole;
        this.isActive = isActive;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEanCode() { return eanCode; }
    public void setEanCode(String eanCode) { this.eanCode = eanCode; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public String getMarketRole() { return marketRole; }
    public void setMarketRole(String marketRole) { this.marketRole = marketRole; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static BrpRegisterEntryBuilder builder() {
        return new BrpRegisterEntryBuilder();
    }

    public static class BrpRegisterEntryBuilder {
        private Long id;
        private String eanCode;
        private String partyName;
        private String marketRole;
        private Boolean isActive = true;
        private LocalDate validFrom;
        private LocalDate validTo;
        private LocalDateTime createdAt;

        BrpRegisterEntryBuilder() {}

        public BrpRegisterEntryBuilder id(Long id) { this.id = id; return this; }
        public BrpRegisterEntryBuilder eanCode(String eanCode) { this.eanCode = eanCode; return this; }
        public BrpRegisterEntryBuilder partyName(String partyName) { this.partyName = partyName; return this; }
        public BrpRegisterEntryBuilder marketRole(String marketRole) { this.marketRole = marketRole; return this; }
        public BrpRegisterEntryBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public BrpRegisterEntryBuilder validFrom(LocalDate validFrom) { this.validFrom = validFrom; return this; }
        public BrpRegisterEntryBuilder validTo(LocalDate validTo) { this.validTo = validTo; return this; }
        public BrpRegisterEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public BrpRegisterEntry build() {
            return new BrpRegisterEntry(id, eanCode, partyName, marketRole, isActive,
                    validFrom, validTo, createdAt);
        }
    }
}
