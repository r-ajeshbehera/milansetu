package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {
    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected RevokedToken() { }

    public RevokedToken(String jti, LocalDateTime expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
