package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.CaseFile;
import com.rajesh.milansetu.entity.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {

    @Query("""
           select c from CaseFile c join fetch c.event
             left join fetch c.missingReport mr left join fetch c.foundReport fr
             left join fetch c.member m left join fetch m.guardian
             left join fetch c.assignedDesk
           where c.id = :id
           """)
    Optional<CaseFile> findWithDetails(@Param("id") Long id);

    @Query("select c from CaseFile c where c.foundReport.id = :foundId and c.mergedInto is null")
    Optional<CaseFile> findCanonicalByFound(@Param("foundId") Long foundId);

    @Query("select c from CaseFile c where c.mergedInto.id = :id")
    List<CaseFile> findMergedInto(@Param("id") Long id);

    @Query("select c from CaseFile c where c.missingReport.id = :missingId and c.mergedInto is null")
    Optional<CaseFile> findCanonicalByMissing(@Param("missingId") Long missingId);

    /** Staff view: canonical cases of the given events. */
    @Query(value = """
           select c from CaseFile c
           where c.mergedInto is null and c.event.id in :eventIds
             and (:status is null or c.status = :status)
             and (:deskId is null or c.assignedDesk.id = :deskId)
             and (:from is null or c.openedAt >= :from) and (:to is null or c.openedAt < :to)
           """,
           countQuery = """
           select count(c) from CaseFile c
           where c.mergedInto is null and c.event.id in :eventIds
             and (:status is null or c.status = :status)
             and (:deskId is null or c.assignedDesk.id = :deskId)
             and (:from is null or c.openedAt >= :from) and (:to is null or c.openedAt < :to)
           """)
    Page<CaseFile> searchForStaff(@Param("eventIds") Collection<Long> eventIds, @Param("status") CaseStatus status,
                                  @Param("deskId") Long deskId, @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to, Pageable pageable);

    /** Family view: cases about their registered members or their own missing reports. */
    @Query(value = """
           select c from CaseFile c left join c.member m left join c.missingReport mr
           where c.mergedInto is null
             and (m.guardian.id = :userId or mr.reportedBy.id = :userId)
           """,
           countQuery = """
           select count(c) from CaseFile c left join c.member m left join c.missingReport mr
           where c.mergedInto is null and (m.guardian.id = :userId or mr.reportedBy.id = :userId)
           """)
    Page<CaseFile> searchForFamily(@Param("userId") Long userId, Pageable pageable);

    @Query("select c.status, count(c) from CaseFile c where c.mergedInto is null and c.event.id = :eventId group by c.status")
    List<Object[]> countByStatus(@Param("eventId") Long eventId);

    @Query("select c from CaseFile c where c.mergedInto is null and c.event.id = :eventId and c.reunitedAt is not null")
    List<CaseFile> findReunited(@Param("eventId") Long eventId);

    @Query("select c from CaseFile c where c.mergedInto is null and c.event.id = :eventId and (:from is null or c.openedAt >= :from)")
    List<CaseFile> findOpenedSince(@Param("eventId") Long eventId, @Param("from") LocalDateTime from);

    /** Stale = still open, not yet flagged, and no timeline activity since the threshold. */
    @Query("""
           select c from CaseFile c
           where c.mergedInto is null and c.status in :statuses and c.staleFlaggedAt is null
             and (select max(e.changedAt) from CaseEvent e where e.caseId = c.id) < :threshold
           """)
    List<CaseFile> findStale(@Param("statuses") Collection<CaseStatus> statuses, @Param("threshold") LocalDateTime threshold);
}
