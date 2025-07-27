package org.avi.utils;

public class SnowflakeIdGenerator {
    private final long nodeId;
    private final long customEpoch = 1659312000000L; // Set your epoch
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long nodeId) {
        if (nodeId < 0 || nodeId > 1023) throw new IllegalArgumentException("NodeId out of bounds");
        this.nodeId = nodeId;
    }

    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp < lastTimestamp) throw new RuntimeException("Clock moved backwards");
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & 0xFFF;
            if (sequence == 0) currentTimestamp = waitNextMillis(currentTimestamp);
        } else {
            sequence = 0;
        }
        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - customEpoch) << 22) | (nodeId << 12) | sequence;
    }

    private long waitNextMillis(long timestamp) {
        while (timestamp == lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}


