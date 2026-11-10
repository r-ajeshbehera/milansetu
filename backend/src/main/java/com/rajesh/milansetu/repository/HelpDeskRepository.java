package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.HelpDesk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HelpDeskRepository extends JpaRepository<HelpDesk, Long> {
    @Query("select d from HelpDesk d left join fetch d.inCharge where d.event.id = :eventId order by d.name")
    List<HelpDesk> findForEvent(@Param("eventId") Long eventId);

    List<HelpDesk> findByEvent_IdAndActiveTrue(Long eventId);

    @Query("select d from HelpDesk d join fetch d.event where d.id = :id")
    Optional<HelpDesk> findWithEvent(@Param("id") Long id);
}
