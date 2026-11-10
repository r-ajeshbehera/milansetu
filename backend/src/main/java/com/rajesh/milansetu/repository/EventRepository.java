package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByActiveTrueOrderByStartDateDesc();

    boolean existsByNameIgnoreCase(String name);
}
