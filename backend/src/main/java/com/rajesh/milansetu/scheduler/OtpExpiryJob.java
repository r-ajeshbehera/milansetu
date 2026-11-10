package com.rajesh.milansetu.scheduler;

import com.rajesh.milansetu.repository.HandoverOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Every minute: ACTIVE codes past their expiry become EXPIRED (idempotent bulk update).
 * Correctness does not depend on this job: verification checks expires_at itself.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OtpExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(OtpExpiryJob.class);
    private final HandoverOtpRepository otps;

    public OtpExpiryJob(HandoverOtpRepository otps) {
        this.otps = otps;
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void run() {
        int n = otps.expireOverdue(LocalDateTime.now());
        if (n > 0) log.info("Expired {} OTP(s)", n);
    }
}
