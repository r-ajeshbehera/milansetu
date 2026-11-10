package com.rajesh.milansetu.unit;

import com.rajesh.milansetu.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entity builders for pure unit tests (no database). */
public final class TestData {
    private TestData() { }

    public static final LocalDateTime T0 = LocalDateTime.of(2027, 7, 7, 10, 0);
    public static final User FAMILY = user(6L, Role.FAMILY);
    public static final Event EVENT = event(1L);

    public static User user(Long id, Role role) {
        User u = new User("User " + id, "u" + id + "@x", role == Role.FAMILY ? "9000000000" : null, "hash", role);
        u.setIdForTest(id);
        return u;
    }

    public static Event event(Long id) {
        Event e = new Event("Rath Yatra (TEST)", "Puri", LocalDate.of(2027, 7, 5), LocalDate.of(2027, 7, 15), FAMILY);
        e.setActive(true);
        e.setIdForTest(id);
        return e;
    }

    public static HelpDesk desk(Long id, String name, double lat, double lng) {
        HelpDesk d = new HelpDesk(EVENT, name, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
        d.setIdForTest(id);
        return d;
    }

    public static MissingReport missing(Gender g, int age, ClothingColor c, double lat, double lng, LocalDateTime at) {
        MissingReport m = new MissingReport(EVENT, FAMILY, null, "Child", age, g, c, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), at);
        m.setIdForTest(1L);
        return m;
    }

    public static FoundReport found(Long id, Gender g, int age, ClothingColor c, double lat, double lng, LocalDateTime at) {
        FoundReport f = new FoundReport(EVENT, desk(1L, "Desk", 19.8049, 85.8182), user(3L, Role.DESK), null, FoundSource.MANUAL,
                age, g, c, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), at, null);
        f.setIdForTest(id);
        return f;
    }
}
