package com.rajesh.milansetu.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class Pages {
    static final int MAX_SIZE = 100;

    private Pages() { }

    /** Prevents clients from asking for huge pages. */
    static Pageable capped(Pageable p) {
        return p.getPageSize() <= MAX_SIZE ? p : PageRequest.of(p.getPageNumber(), MAX_SIZE, p.getSort());
    }
}
