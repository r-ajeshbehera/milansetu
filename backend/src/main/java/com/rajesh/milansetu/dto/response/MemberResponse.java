package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Gender;
import com.rajesh.milansetu.entity.Member;

/** Only ever returned to the owning family. The QR token itself is served only as an image. */
public record MemberResponse(Long id, Long eventId, String eventName, String fullName, int age, Gender gender, String notes) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getEventId(), m.getEvent().getName(), m.getFullName(), m.getAge(),
                m.getGender(), m.getNotes());
    }
}
