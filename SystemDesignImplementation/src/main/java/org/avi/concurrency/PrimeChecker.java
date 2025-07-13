package org.avi.concurrency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PrimeChecker {

    public static boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;  // 2 and 3 are prime

        if (n % 2 == 0 || n % 3 == 0) return false;

        // Check from 5 to √n with step of 6
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0)
                return false;
        }
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
//        concurrentPrimeNumber();
        int MAX_INT = 1000000000;
        int numThreads = 8;

        long start = System.currentTimeMillis();
        int primeCount = parallelSieveWithLogs(MAX_INT, numThreads);
        long end = System.currentTimeMillis();

        System.out.println("\n✅ Total primes <= " + MAX_INT + ": " + primeCount);
        System.out.println("🕒 Time taken (ms): " + (end - start));
    }
    private static void nonConcurrentPrimeNumber(int n){
        long startTime = System.currentTimeMillis();
        int count = 0;
        int MAX_INT = 1000000000;
        for (int i = 0; i < MAX_INT; i++) {
            if (PrimeChecker.isPrime(i)) {
                count++;
            }
        }
        System.out.println("Prime count: " + count);
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
//        Prime count: 50847534
//        Time taken: 326354
    }

    private static void concurrentPrimeNumber() throws InterruptedException {
        long startTime = System.currentTimeMillis();

//        int MAX = 10_000_000; // 10 million for demo; increase gradually
        int concurrency = 10;
        AtomicInteger count = new AtomicInteger(0);
        int MAX_INT = 1000000000;
        int batchSize = MAX_INT / concurrency;
        System.out.println("Concurrency: " + concurrency);
        System.out.println("Batch size: " + batchSize);

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            int start = i * batchSize;
            int end = (i == concurrency - 1) ? MAX_INT : start + batchSize;

            Thread thread = new Thread(() -> {
                for (int j = start; j < end; j++) {
                    if (isPrime(j)) {
                        count.incrementAndGet();
                    }
                }
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join(); // wait for all to finish
        }
        System.out.println("Prime count: " + count);
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
//        Concurrency: 10
//        Batch size: 100000000
//        Prime count: 50847534
//        Time taken: 82791
    }

    public static int parallelSieveWithLogs(int n, int numThreads) throws InterruptedException {
        boolean[] isPrime = new boolean[n + 1];
        Arrays.fill(isPrime, true);
        isPrime[0] = false;
        isPrime[1] = false;

        int sqrtN = (int) Math.sqrt(n);

        // Phase 1: Sieve up to sqrt(n)
        for (int i = 2; i <= sqrtN; i++) {
            if (isPrime[i]) {
                for (int j = i * i; j <= sqrtN; j += i) {
                    isPrime[j] = false;
                }
            }
        }

        // Phase 2: Parallel sieve from sqrt(n)+1 to n
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int chunkSize = (n - sqrtN) / numThreads;

        AtomicInteger progress = new AtomicInteger(0);
        int totalChunks = numThreads;

        System.out.println("🚀 Starting parallel sieving with " + numThreads + " threads...");

        for (int t = 0; t < numThreads; t++) {
            int start = sqrtN + 1 + t * chunkSize;
            int end = (t == numThreads - 1) ? n : start + chunkSize - 1;
            int threadId = t;

            executor.submit(() -> {
                long threadStart = System.currentTimeMillis();
                for (int i = 2; i <= sqrtN; i++) {
                    if (isPrime[i]) {
                        int firstMultiple = ((start + i - 1) / i) * i;
                        for (int j = Math.max(firstMultiple, i * i); j <= end; j += i) {
                            isPrime[j] = false;
                        }
                    }
                }
                long threadEnd = System.currentTimeMillis();
                int done = progress.incrementAndGet();
                printProgressBar(done, totalChunks, threadId);
                System.out.printf("🧵 Thread-%d finished chunk [%d, %d] in %d ms%n",
                        threadId, start, end, (threadEnd - threadStart));
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        // Count primes
        int count = 0;
        for (boolean b : isPrime) {
            if (b) count++;
        }
        return count;
    }

    private static void printProgressBar(int done, int total, int threadId) {
        int percent = (int) (((double) done / total) * 100);
        StringBuilder bar = new StringBuilder("[");
        int blocks = percent / 5;
        for (int i = 0; i < 20; i++) {
            bar.append(i < blocks ? "#" : "-");
        }
        bar.append("] ");
        System.out.printf("Thread-%d finished chunk. Progress: %s %d%%%n", threadId, bar, percent);
    }
}
