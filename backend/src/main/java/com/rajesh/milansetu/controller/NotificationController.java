package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.dto.response.NotificationResponse;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.NotificationRepository;
import com.rajesh.milansetu.security.AppUserPrincipal;
import com.rajesh.milansetu.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {
    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public Page<NotificationResponse> mine(@PageableDefault(size = 20) Pageable pageable) {
        return notifications.findByUserIdOrderByCreatedAtDesc(CurrentUser.get().id(), Pages.capped(pageable))
                .map(NotificationResponse::from);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("unread", notifications.countByUserIdAndReadAtIsNull(CurrentUser.get().id()));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> read(@PathVariable Long id) {
        AppUserPrincipal me = CurrentUser.get();
        var n = notifications.findById(id).filter(x -> x.getUserId().equals(me.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.markRead();
        return ResponseEntity.noContent().build();
    }
}
