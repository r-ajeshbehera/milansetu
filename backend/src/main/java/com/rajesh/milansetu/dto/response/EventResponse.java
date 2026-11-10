package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Event;
import java.time.LocalDate;

public record EventResponse(Long id, String name, String city, LocalDate startDate, LocalDate endDate, boolean active) {
    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getCity(), e.getStartDate(), e.getEndDate(), e.isActive());
    }
}
