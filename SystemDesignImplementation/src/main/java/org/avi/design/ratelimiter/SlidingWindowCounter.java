package org.avi.design.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowCounter {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public SlidingWindowCounter(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    public boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        long currentWindow = now / windowSizeMillis;

        WindowCounter counter = counters.computeIfAbsent(clientId, k -> new WindowCounter());

        synchronized (counter) {
            if (counter.windowStart != currentWindow) {
                // Shift the windows
                counter.prevCount = counter.currCount;
                counter.currCount = 0;
                counter.windowStart = currentWindow;
            }

            // Fraction of previous window still in play
            double elapsedInWindow = (double)(now % windowSizeMillis) / windowSizeMillis;
            double weightedCount = counter.currCount + (1 - elapsedInWindow) * counter.prevCount;

            if (weightedCount < maxRequests) {
                counter.currCount++;
                return true; // ✅ allowed
            } else {
                return false; // ❌ too many requests
            }
        }
    }

    private static class WindowCounter {
        long windowStart = -1;
        int currCount = 0;
        int prevCount = 0;
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowCounter limiter = new SlidingWindowCounter(5, 1000); // 5 req/sec
        String client = "client123";

        for (int i = 0; i < 10; i++) {
            boolean allowed = limiter.allowRequest(client);
            System.out.println("Request " + i + " -> " + (allowed ? "✅ allowed" : "❌ rejected"));
            Thread.sleep(150); // 150ms gap
        }
    }

}

