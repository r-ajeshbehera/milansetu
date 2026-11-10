package com.rajesh.milansetu.service;

import com.rajesh.milansetu.entity.AuditLog;
import com.rajesh.milansetu.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

/** Writes audit rows inside the caller's transaction. Never pass personal data in "details". */
@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(Long actorId, String action, String entityType, Long entityId, String details) {
        repository.save(new AuditLog(actorId, action, entityType, entityId, details));
    }
}
