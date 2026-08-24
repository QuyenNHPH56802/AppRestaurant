package com.restaurant.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * V2.2 / V12: In-app notification for a single user.
 * Titles/bodies are stored in vi + ko inline so they render regardless of the
 * user's current locale (notifications should look the same as when sent).
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "INTEGER")
    private Long userId;

    /** Free-form type; controlled by an application enum. */
    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "title_vi", nullable = false)
    private String titleVi;

    @Column(name = "title_ko", nullable = false)
    private String titleKo;

    @Column(name = "body_vi")
    private String bodyVi;

    @Column(name = "body_ko")
    private String bodyKo;

    @Column(name = "payload_json")
    private String payloadJson;

    /** NULL until the user reads the notification. */
    @Column(name = "read_at")
    private Instant readAt;

    /** V2.3 / V18: nullable. When set, must be unique (PARTIAL UNIQUE INDEX). */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitleVi() { return titleVi; }
    public void setTitleVi(String titleVi) { this.titleVi = titleVi; }
    public String getTitleKo() { return titleKo; }
    public void setTitleKo(String titleKo) { this.titleKo = titleKo; }
    public String getBodyVi() { return bodyVi; }
    public void setBodyVi(String bodyVi) { this.bodyVi = bodyVi; }
    public String getBodyKo() { return bodyKo; }
    public void setBodyKo(String bodyKo) { this.bodyKo = bodyKo; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
