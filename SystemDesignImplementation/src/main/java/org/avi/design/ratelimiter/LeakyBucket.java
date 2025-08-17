package org.avi.design.ratelimiter;

import java.util.concurrent.locks.ReentrantLock;

public class LeakyBucket {
    private final int capacity;              // max bucket size
    private final double leakRatePerSecond;  // requests per second leak rate
    private double water;                    // current "water" in bucket
    private long lastUpdate;                 // last leak timestamp (ns)
    private final ReentrantLock lock = new ReentrantLock();

    public LeakyBucket(int capacity, double leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
        this.water = 0;
        this.lastUpdate = System.nanoTime();
    }

    /**
     * Try to add a request (1 unit). Returns true if accepted, false if rejected.
     */
    public boolean allowRequest() {
        lock.lock();
        try {
            leak();
            if (water < capacity) {
                water += 1; // add one request
                return true;
            } else {
                return false; // bucket overflow
            }
        } finally {
            lock.unlock();
        }
    }

    private void leak() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastUpdate;
        if (elapsedNanos <= 0) return;

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double leaked = elapsedSeconds * leakRatePerSecond;

        if (leaked > 0) {
            water = Math.max(0, water - leaked);
            lastUpdate = now;
        }
    }

    public double getWaterLevel() {
        lock.lock();
        try {
            leak();
            return water;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LeakyBucket bucket = new LeakyBucket(10, 2); // cap=10, leak=2 req/s

        for (int i = 0; i < 15; i++) {
            boolean allowed = bucket.allowRequest();
            System.out.println("Req " + i + " -> " + (allowed ? "✅ allowed" : "❌ rejected"));
            Thread.sleep(200); // 5 req/sec arrival
        }
    }
}

