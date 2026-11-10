package com.rajesh.milansetu.scheduler;

import com.rajesh.milansetu.config.AppProperties;
import com.rajesh.milansetu.entity.CaseFile;
import com.rajesh.milansetu.entity.CaseStatus;
import com.rajesh.milansetu.entity.NotificationType;
import com.rajesh.milansetu.notification.NotificationService;
import com.rajesh.milansetu.repository.CaseFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Every 15 minutes: flags open cases with no activity for N minutes so police and desks follow up.
 * It NEVER closes a case automatically: a missing child must not disappear from the list because a timer ran out.
 * Idempotent: a flagged case is not flagged again until its status changes (moveTo() clears the flag).
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaleCaseJob {
    private static final Logger log = LoggerFactory.getLogger(StaleCaseJob.class);
    private final CaseFileRepository cases;
    private final NotificationService notifications;
    private final int staleAfterMinutes;

    public StaleCaseJob(CaseFileRepository cases, NotificationService notifications, AppProperties props) {
        this.cases = cases;
        this.notifications = notifications;
        this.staleAfterMinutes = props.jobs().staleAfterMinutes();
    }

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT2M")
    @Transactional
    public void run() {
        List<CaseFile> stale = cases.findStale(EnumSet.of(CaseStatus.REPORTED, CaseStatus.MATCHED, CaseStatus.AT_DESK),
                LocalDateTime.now().minusMinutes(staleAfterMinutes));
        for (CaseFile c : stale) {
            c.flagStale();
            if (c.getAssignedDesk() != null && c.getAssignedDesk().getInCharge() != null) {
                notifications.notify(c.getAssignedDesk().getInCharge(), c.getId(), NotificationType.STATUS_CHANGED,
                        "Case " + c.getId() + " has had no update for " + staleAfterMinutes + " minutes. Please follow up.");
            }
        }
        if (!stale.isEmpty()) log.info("Flagged {} stale case(s)", stale.size());
    }
}
