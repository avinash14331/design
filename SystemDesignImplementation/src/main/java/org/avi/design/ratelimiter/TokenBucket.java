package org.avi.design.ratelimiter;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {
    private final double capacity;
    private final double refillRatePerSecond; // tokens per second
    private double tokens;
    private long lastRefillNanos;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(double capacity, double refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = capacity; // start full
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Try to consume 'amount' tokens. Returns true if request is allowed.
     */
    public boolean tryConsume(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        lock.lock();
        try {
            refill();  // just refills, no return
            if (tokens >= amount) {
                tokens -= amount;
                return true; // ✅ enough tokens
            }
            return false; // ❌ not enough tokens
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refill tokens based on elapsed time.
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos <= 0) return;

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double refillTokens = elapsedSeconds * refillRatePerSecond;

        if (refillTokens > 0) {
            tokens = Math.min(capacity, tokens + refillTokens);
            lastRefillNanos = now;
        }
    }

    public double getAvailableTokens() {
        lock.lock();
        try {
            refill();
            return tokens;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        TokenBucket tokenBucket = new TokenBucket(10, 1);
        for (int i = 0; i < 12; i++) {
            System.out.println(tokenBucket.tryConsume(1));
        }
    }
}

