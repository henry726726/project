package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_result_json")
public class AdResultJson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: ad_contents.id
    @Column(name = "ad_content_id", nullable = false)
    private Long adContentId;

    @Column(name = "json_type", length = 32, nullable = false)
    private String jsonType;

    // MySQL JSON 컬럼 (문자열로 보관)
    @Column(columnDefinition = "JSON", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AdResultJson() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ----- getters / setters -----
    public Long getId() { return id; }

    public Long getAdContentId() { return adContentId; }
    public void setAdContentId(Long adContentId) { this.adContentId = adContentId; }

    public String getJsonType() { return jsonType; }
    public void setJsonType(String jsonType) { this.jsonType = jsonType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
