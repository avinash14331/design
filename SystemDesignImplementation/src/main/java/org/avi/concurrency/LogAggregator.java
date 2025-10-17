package org.avi.concurrency;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LogAggregator {
    Map<String, Deque<String>> logMap;
    int capacity;
    public LogAggregator( int capacity) {
        logMap = new ConcurrentHashMap<>();
        this.capacity = capacity;
    }
    public static void main(String[] args) {
        LogAggregator agg = new LogAggregator(3);
        agg.addLog("node1", "A");
        agg.addLog("node1", "B");
        agg.addLog("node1", "C");
        agg.addLog("node1", "D");
        System.out.println(agg.getRecentLogs("node1"));  // returns ["D", "C", "B"]
        System.out.println(agg.getAllNodes());
    }
    public void addLog(String nodeId, String logLine) {
        // Atomic computeIfAbsent ensures thread-safe creation
        Deque<String> deque = logMap.computeIfAbsent(nodeId, k -> new ConcurrentLinkedDeque<>());

        synchronized (deque) {  // fine-grained lock, only per node
            if (deque.size() >= capacity) {
                deque.removeFirst(); // remove oldest
            }
            deque.addLast(logLine);
        }
    }

    public List<String> getRecentLogs(String nodeId) {
        Deque<String> deque = logMap.get(nodeId);
        if (deque == null) return Collections.emptyList();

        synchronized (deque) {
            List<String> result = new ArrayList<>(deque);
            Collections.reverse(result);
            return result;
        }
    }

    public List<String> getAllNodes() {
        return new ArrayList<>(logMap.keySet());
    }
}
