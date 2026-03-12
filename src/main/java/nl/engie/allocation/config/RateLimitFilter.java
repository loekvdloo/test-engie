package nl.engie.allocation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Eenvoudige in-memory rate limiter per IP-adres.
 *
 * <p>Beschermt tegen:
 * <ul>
 *   <li>DoS aanvallen via herhaalde POST requests</li>
 *   <li>Spam van de test-seeder endpoint</li>
 *   <li>Database overbelasting door massale berichtinzendingen</li>
 * </ul>
 *
 * <p>Limieten:
 * <ul>
 *   <li>POST /api/messages: max 30 per minuut per IP</li>
 *   <li>POST /api/test/seed: max 5 per minuut per IP</li>
 *   <li>Overige: max 120 per minuut per IP</li>
 * </ul>
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int WINDOW_MS = 60_000; // 1 minuut
    private static final int MAX_SUBMIT_REQUESTS = 30;
    private static final int MAX_SEED_REQUESTS = 5;
    private static final int MAX_GENERAL_REQUESTS = 120;

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        int limit;
        String bucket;

        if ("POST".equals(method) && path.startsWith("/api/test/seed")) {
            bucket = clientIp + ":seed";
            limit = MAX_SEED_REQUESTS;
        } else if ("POST".equals(method) && path.startsWith("/api/messages")) {
            bucket = clientIp + ":submit";
            limit = MAX_SUBMIT_REQUESTS;
        } else if (path.startsWith("/api/")) {
            bucket = clientIp + ":general";
            limit = MAX_GENERAL_REQUESTS;
        } else {
            // Statische bestanden (CSS, JS, HTML) niet rate-limiten
            chain.doFilter(request, response);
            return;
        }

        RateWindow window = windows.compute(bucket, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateWindow(now);
            }
            return existing;
        });

        int count = window.counter.incrementAndGet();

        if (count > limit) {
            log.warn("Rate limit overschreden voor {} op {} ({}/{})", clientIp, path, count, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Te veel verzoeken. Probeer het over een minuut opnieuw.\",\"retryAfterSeconds\":60}"
            );
            return;
        }

        // Voeg rate-limit headers toe
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateWindow {
        final long windowStart;
        final AtomicInteger counter;

        RateWindow(long windowStart) {
            this.windowStart = windowStart;
            this.counter = new AtomicInteger(0);
        }
    }
}
