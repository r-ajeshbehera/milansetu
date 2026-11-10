package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.HandoverOtp;
import com.rajesh.milansetu.entity.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface HandoverOtpRepository extends JpaRepository<HandoverOtp, Long> {
    Optional<HandoverOtp> findFirstByCaseIdOrderByCreatedAtDesc(Long caseId);

    Optional<HandoverOtp> findByCaseIdAndStatus(Long caseId, OtpStatus status);

    long countByCaseIdAndCreatedAtAfter(Long caseId, LocalDateTime after);

    @Modifying
    @Query("update HandoverOtp o set o.status = com.rajesh.milansetu.entity.OtpStatus.EXPIRED where o.status = com.rajesh.milansetu.entity.OtpStatus.ACTIVE and o.expiresAt < :now")
    int expireOverdue(@Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from HandoverOtp o where o.status <> com.rajesh.milansetu.entity.OtpStatus.ACTIVE and o.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
