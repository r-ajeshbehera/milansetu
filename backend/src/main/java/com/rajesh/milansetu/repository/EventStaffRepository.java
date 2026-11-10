package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.EventStaff;
import com.rajesh.milansetu.entity.EventStaffId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventStaffRepository extends JpaRepository<EventStaff, EventStaffId> {
    List<EventStaff> findByIdUserId(Long userId);

    List<EventStaff> findByIdEventId(Long eventId);
}
