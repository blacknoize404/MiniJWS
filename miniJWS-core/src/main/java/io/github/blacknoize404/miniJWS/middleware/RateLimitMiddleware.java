package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.Middleware;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RateLimitMiddleware implements Middleware {

    private final int maxRequests;
    private final Duration window;
    private final ConcurrentHashMap<String, Queue<Instant>> requests = new ConcurrentHashMap<>();
    private final AtomicInteger totalEntries = new AtomicInteger(0);
    private static final int CLEANUP_THRESHOLD = 10_000;

    public RateLimitMiddleware(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public HttpResponse run(HttpRequest request, MiddlewareChain chain) {
        String ip = extractClientIp(request);
        Instant now = Instant.now();

        Queue<Instant> timestamps = requests.computeIfAbsent(ip, k -> {
            totalEntries.incrementAndGet();
            return new ConcurrentLinkedQueue<>();
        });

        synchronized (timestamps) {
            Instant cutoff = now.minus(window);
            while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
                timestamps.poll();
            }

            if (timestamps.size() >= maxRequests) {
                return new HttpResponse.Builder()
                        .setStatusCode(429)
                        .setContentType(ContentType.TEXT)
                        .addHeader("Retry-After", String.valueOf(window.toSeconds()))
                        .setBody("429 - Too Many Requests")
                        .build();
            }

            timestamps.add(now);
        }

        if (totalEntries.get() > CLEANUP_THRESHOLD) {
            cleanupStaleEntries(now.minus(window));
        }

        return chain.next(request);
    }

    private void cleanupStaleEntries(Instant cutoff) {
        int removed = 0;
        for (var it = requests.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            Queue<Instant> q = entry.getValue();
            synchronized (q) {
                while (!q.isEmpty() && q.peek().isBefore(cutoff)) {
                    q.poll();
                }
                if (q.isEmpty()) {
                    it.remove();
                    removed++;
                }
            }
        }
        totalEntries.addAndGet(-removed);
    }

    private static String extractClientIp(HttpRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For").orElse("");
        if (!forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getHeader("X-Real-IP").orElse("127.0.0.1");
    }
}
