package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.FoundReport;
import com.rajesh.milansetu.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FoundReportRepository extends JpaRepository<FoundReport, Long> {
    /** Matching candidates: uses the (event_id, status) index. */
    @Query("select f from FoundReport f join fetch f.desk left join fetch f.member where f.event.id = :eventId and f.status = :status")
    List<FoundReport> findCandidates(@Param("eventId") Long eventId, @Param("status") ReportStatus status);

    @Query("select f from FoundReport f join fetch f.event join fetch f.desk left join fetch f.member where f.id = :id")
    Optional<FoundReport> findWithDetails(@Param("id") Long id);

    @Query("select f from FoundReport f where f.member.id = :memberId and f.status in :statuses")
    Optional<FoundReport> findActiveForMember(@Param("memberId") Long memberId, @Param("statuses") Collection<ReportStatus> statuses);

    @Query("""
           select f from FoundReport f
           where f.event.id in :eventIds and (:status is null or f.status = :status)
           """)
    Page<FoundReport> search(@Param("eventIds") Collection<Long> eventIds, @Param("status") ReportStatus status, Pageable pageable);

    @Query("""
           select count(f) from FoundReport f where f.event.id = :eventId
             and (:from is null or f.foundAt >= :from) and (:to is null or f.foundAt < :to)
           """)
    long countForEvent(@Param("eventId") Long eventId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select f.desk.name, count(f) from FoundReport f where f.event.id = :eventId group by f.desk.name order by count(f) desc")
    List<Object[]> countByDesk(@Param("eventId") Long eventId);
}
