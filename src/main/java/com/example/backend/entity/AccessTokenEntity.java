package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
public class AccessTokenEntity {

    @Id
    private String userId;

    @Column(length = 1000)
    private String accessToken;

    public AccessTokenEntity() {
    }

    public AccessTokenEntity(String userId, String accessToken) {
        this.userId = userId;
        this.accessToken = accessToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
