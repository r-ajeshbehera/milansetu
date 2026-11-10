package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.CaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {
    @Query("select e from CaseEvent e left join fetch e.changedBy where e.caseId in :caseIds order by e.changedAt, e.id")
    List<CaseEvent> findTimeline(@Param("caseIds") Collection<Long> caseIds);
}
