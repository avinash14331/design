package org.avi.concurrency;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public class MetricsCollector {
//    Map<String, Deque<Double>> metrics;
    int size;
    Map<String, MetricWindow> metricWindowMap;
    DoubleAdder sum = new DoubleAdder();
    AtomicLong totalRecords = new AtomicLong();
    AtomicLong droppedRecords = new AtomicLong();
    public MetricsCollector(int size) {
//        metrics = new ConcurrentHashMap<>();
        metricWindowMap = new ConcurrentHashMap<>();
        this.size = size;
    }
    public static void main(String[] args) {

    }
    public void record(String metricName, double value) {
        MetricWindow window = metricWindowMap.computeIfAbsent(metricName, k -> new MetricWindow());
        synchronized (window) {
            if (window.values.size() >= size) {
                double removed = window.values.removeFirst();
                sum.add(-removed);
                droppedRecords.incrementAndGet();
            }
            window.values.addLast(value);
            sum.add(value);
            totalRecords.incrementAndGet();
        }
    }
    public double getAverage(String metricName) {
        MetricWindow window = metricWindowMap.get(metricName);
        if (window == null) return 0.0;
        synchronized (window) {
            return window.values.isEmpty() ? 0.0 :
                    window.sum.sum() / window.values.size();
        }
    }
    public Map<String, Long> getStats() {
        return Map.of(
                "totalMetrics", (long) metricWindowMap.size(),
                "totalRecords", totalRecords.get(),
                "droppedRecords", droppedRecords.get()
        );
    }

}

class MetricWindow {
    Deque<Double> values = new ArrayDeque<>();
    DoubleAdder sum = new DoubleAdder();
}
