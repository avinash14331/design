package org.avi.data.structures;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<E> implements BlockingQueue<Object> {
    private final Queue<Object> queue = new LinkedList<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    public MyBlockingQueue(int capacity) {
        this.capacity = capacity;
    }
    @Override
    public boolean add(Object o) {
        if (offer(o))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    @Override
    public boolean offer(Object o) {
        Objects.requireNonNull(o);
        lock.lock();
        try {
            if (capacity == queue.size())
                return false;
            else {
                queue.add(o);
                notEmpty.signal();
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object remove() {
        return null;
    }

    @Override
    public Object poll() {
        return null;
    }

    @Override
    public Object element() {
        return null;
    }

    @Override
    public Object peek() {
        return null;
    }

    @Override
    public void put(Object o) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.add(o);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(Object o, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(o);
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (capacity == queue.size()) {
                if (nanos <= 0L)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            queue.add(o);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            E item = (E) queue.remove();
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(Collection c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean retainAll(Collection c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection c) {
        return false;
    }

    @Override
    public boolean containsAll(Collection c) {
        return false;
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] a) {
        return new Object[0];
    }

    @Override
    public int drainTo(Collection c) {
        return 0;
    }

    @Override
    public int drainTo(Collection c, int maxElements) {
        return 0;
    }
}
