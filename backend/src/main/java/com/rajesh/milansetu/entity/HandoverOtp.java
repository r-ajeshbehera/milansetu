package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * A one-time handover code. Only the BCrypt hash is stored.
 * The verification rules live in verify() so they can be unit-tested without Spring.
 */
@Entity
@Table(name = "handover_otps")
public class HandoverOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "otp_hash", nullable = false, length = 60)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OtpStatus status = OtpStatus.ACTIVE;

    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Version
    private Long version;

    protected HandoverOtp() { }

    public HandoverOtp(Long caseId, String otpHash, LocalDateTime expiresAt, Long issuedBy) {
        this.caseId = caseId;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.issuedBy = issuedBy;
        this.createdAt = LocalDateTime.now();
    }

    public enum Result { SUCCESS, WRONG_CODE, LOCKED_NOW, EXPIRED, ALREADY_USED, LOCKED, NOT_ACTIVE }

    /**
     * Checks a code. Order matters: state first (used/locked/expired), then the code itself.
     * @param matches compares the plain code with the stored hash (BCrypt in production, a lambda in tests)
     */
    public Result verify(String code, LocalDateTime now, Predicate<String> matches, int maxAttempts) {
        switch (status) {
            case USED: return Result.ALREADY_USED;
            case LOCKED: return Result.LOCKED;
            case EXPIRED: return Result.EXPIRED;
            case SUPERSEDED: return Result.NOT_ACTIVE;
            default: break;
        }
        if (now.isAfter(expiresAt)) {
            status = OtpStatus.EXPIRED;
            return Result.EXPIRED;
        }
        if (code != null && code.matches("\\d{6}") && matches.test(code)) {
            status = OtpStatus.USED;
            usedAt = now;
            return Result.SUCCESS;
        }
        attempts++;
        if (attempts >= maxAttempts) {
            attempts = maxAttempts;
            status = OtpStatus.LOCKED;
            return Result.LOCKED_NOW;
        }
        return Result.WRONG_CODE;
    }

    public void supersede() {
        if (status == OtpStatus.ACTIVE) status = OtpStatus.SUPERSEDED;
    }

    public int attemptsLeft(int maxAttempts) { return Math.max(0, maxAttempts - attempts); }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public String getOtpHash() { return otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public OtpStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
