package org.avi.concurrency;

import org.avi.data.structures.MyBlockingQueue;

public class ProducerConsumer {
    public static void main(String[] args) {
        MyBlockingQueue<Object> myBlockingQueue = new MyBlockingQueue<>(5);
        Thread producerThread = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("Producer " + i);
                try {
                    myBlockingQueue.put(i);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread consumerThread = new Thread(() -> {
            for (int i = 0; i < 6; i++) {
                try {
                    Object val = myBlockingQueue.take();
                    System.out.println("Consuming " + val);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        producerThread.start();
        consumerThread.start();
    }
}
