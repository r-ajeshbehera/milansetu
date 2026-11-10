package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.MissingReport;
import com.rajesh.milansetu.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface MissingReportRepository extends JpaRepository<MissingReport, Long> {
    @Query("select r from MissingReport r join fetch r.event left join fetch r.member where r.id = :id")
    Optional<MissingReport> findWithDetails(@Param("id") Long id);

    @Query("select r from MissingReport r where r.member.id = :memberId and r.status in :statuses")
    Optional<MissingReport> findActiveForMember(@Param("memberId") Long memberId, @Param("statuses") Collection<ReportStatus> statuses);

    @Query("select r from MissingReport r where r.reportedBy.id = :reporterId and (:status is null or r.status = :status)")
    Page<MissingReport> searchForReporter(@Param("reporterId") Long reporterId, @Param("status") ReportStatus status,
                                          Pageable pageable);

    @Query("select r from MissingReport r where r.event.id in :eventIds and (:status is null or r.status = :status)")
    Page<MissingReport> searchForEvents(@Param("eventIds") Collection<Long> eventIds, @Param("status") ReportStatus status,
                                        Pageable pageable);

    @Query("""
           select count(r) from MissingReport r where r.event.id = :eventId
             and (:from is null or r.lastSeenAt >= :from) and (:to is null or r.lastSeenAt < :to)
           """)
    long countForEvent(@Param("eventId") Long eventId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
