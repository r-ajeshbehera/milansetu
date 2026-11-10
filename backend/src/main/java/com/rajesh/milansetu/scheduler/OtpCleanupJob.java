package com.rajesh.milansetu.scheduler;

import com.rajesh.milansetu.config.AppProperties;
import com.rajesh.milansetu.repository.HandoverOtpRepository;
import com.rajesh.milansetu.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Daily retention clean-up: old non-active OTP rows and expired revoked tokens. */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OtpCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(OtpCleanupJob.class);
    private final HandoverOtpRepository otps;
    private final RevokedTokenRepository revoked;
    private final int retentionDays;

    public OtpCleanupJob(HandoverOtpRepository otps, RevokedTokenRepository revoked, AppProperties props) {
        this.otps = otps;
        this.revoked = revoked;
        this.retentionDays = props.otp().retentionDays();
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void run() {
        int deleted = otps.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
        int tokens = revoked.deleteExpired(LocalDateTime.now(ZoneOffset.UTC));
        log.info("Retention clean-up: {} OTP rows, {} revoked tokens removed", deleted, tokens);
    }
}
