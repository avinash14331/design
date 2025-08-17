package org.avi.design.ratelimiter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowLog {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final ConcurrentHashMap<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    public SlidingWindowLog(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    public boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestLogs.computeIfAbsent(clientId, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // 1. Remove expired requests
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= windowSizeMillis) {
                timestamps.pollFirst();
            }

            // 2. Check count
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now); // record this request
                return true; // ✅ allowed
            } else {
                return false; // ❌ too many requests
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowLog limiter = new SlidingWindowLog(5, 1000); // 5 req / sec
        String client = "client123";

        for (int i = 0; i < 10; i++) {
            boolean allowed = limiter.allowRequest(client);
            System.out.println("Request " + i + " -> " + (allowed ? "✅ allowed" : "❌ rejected"));
            Thread.sleep(150); // 150ms gap
        }
    }

}

