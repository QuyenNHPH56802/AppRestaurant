package com.restaurant.server.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * V2.2 / V7: A physical/logical area in the restaurant (e.g. Bếp phở, Bún chả).
 * Soft-disable via {@link Status#DISABLED}; never hard delete (RESTRICT).
 */
@Entity
@Table(name = "zones")
public class Zone {

    public enum Status { ACTIVE, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /** HEX color for the manager dashboard. */
    @Column(name = "color", nullable = false)
    private String color = "#3B82F6";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "required_staff", nullable = false)
    private Integer requiredStaff = 1;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "qr_generated_at")
    private Instant qrGeneratedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("languageCode ASC")
    private List<ZoneTranslation> translations = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (sortOrder == null) sortOrder = 0;
        if (requiredStaff == null) requiredStaff = 1;
        if (color == null) color = "#3B82F6";
        if (status == null) status = Status.ACTIVE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getRequiredStaff() { return requiredStaff; }
    public void setRequiredStaff(Integer requiredStaff) { this.requiredStaff = requiredStaff; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public Instant getQrGeneratedAt() { return qrGeneratedAt; }
    public void setQrGeneratedAt(Instant qrGeneratedAt) { this.qrGeneratedAt = qrGeneratedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<ZoneTranslation> getTranslations() { return translations; }
    public void setTranslations(List<ZoneTranslation> translations) { this.translations = translations; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Zone other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
