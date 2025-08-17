package org.avi.design.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowCounter {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final ConcurrentHashMap<String, Window> clients = new ConcurrentHashMap<>();

    public FixedWindowCounter(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    public boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        Window window = clients.computeIfAbsent(clientId, k -> new Window(now, new AtomicInteger(0)));

        synchronized (window) {
            if (now - window.windowStart >= windowSizeMillis) {
                // Reset for new window
                window.windowStart = now;
                window.counter.set(0);
            }

            if (window.counter.get() < maxRequests) {
                window.counter.incrementAndGet();
                return true; // ✅ allowed
            } else {
                return false; // ❌ rejected
            }
        }
    }

    private static class Window {
        long windowStart;
        AtomicInteger counter;

        Window(long start, AtomicInteger counter) {
            this.windowStart = start;
            this.counter = counter;
        }
    }
    public static void main(String[] args) throws InterruptedException {
        FixedWindowCounter limiter = new FixedWindowCounter(5, 1000); // 5 req per 1s

        String client = "client123";

        for (int i = 0; i < 10; i++) {
            boolean allowed = limiter.allowRequest(client);
            System.out.println("Request " + i + " -> " + (allowed ? "✅ allowed" : "❌ rejected"));
            Thread.sleep(150); // 150 ms between requests
        }
    }

}

