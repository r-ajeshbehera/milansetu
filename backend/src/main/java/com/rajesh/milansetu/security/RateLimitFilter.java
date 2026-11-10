package com.rajesh.milansetu.security;

import com.rajesh.milansetu.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple fixed-window rate limiter per client IP (no external library, no Redis).
 * Login/register: M attempts/minute. Scan, OTP issue and handover: N requests/minute (slows OTP guessing).
 * Limitation (documented): counters live in memory, so they are per instance and reset on restart.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int publicLimit;
    private final int loginLimit;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired   // two constructors: tell Spring which one to use (the other is for tests)
    public RateLimitFilter(AppProperties props) {
        this(props.rateLimit().publicPerMinute(), props.rateLimit().loginPerMinute(), Clock.systemUTC());
    }

    RateLimitFilter(int publicLimit, int loginLimit, Clock clock) {
        this.publicLimit = publicLimit;
        this.loginLimit = loginLimit;
        this.clock = clock;
    }

    private record Window(long minute, AtomicInteger count) { }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.equals("/api/auth/login") || uri.equals("/api/auth/register")
                || uri.startsWith("/api/scan/") || uri.endsWith("/handover") || uri.endsWith("/otp"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean auth = uri.startsWith("/api/auth/");
        int limit = auth ? loginLimit : publicLimit;
        // per-endpoint-type bucket: auth (login/register) vs sensitive actions (scan, OTP, handover)
        String key = (auth ? "auth:" : "sensitive:") + clientIp(request);
        if (!tryAcquire(key, limit)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Try again in a minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    boolean tryAcquire(String key, int limit) {
        long minute = clock.millis() / 60_000;
        Window w = windows.compute(key, (k, old) -> (old == null || old.minute() != minute)
                ? new Window(minute, new AtomicInteger()) : old);
        if (windows.size() > 50_000) windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        return w.count().incrementAndGet() <= limit;
    }

    /** Behind Nginx the real client IP arrives in X-Forwarded-For (Nginx overwrites it, so it cannot be spoofed). */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
