package org.avi.data.structures.database;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;

// Robust lock-free skiplist with upsert support
public class LockFreeSkipList<T> {
    private static final int MAX_LEVEL = 16;
    private static final double P = 0.5;
    private final Node<T> head;
    private final Node<T> tail;

    static class Node<T> {
        final int key;
        volatile T value; // Made volatile for thread-safe updates
        final AtomicReference<Node<T>>[] forward;
        final int level;
        volatile boolean marked;

        @SuppressWarnings("unchecked")
        Node(int key, T value, int level) {
            this.key = key;
            this.value = value;
            this.level = level;
            this.marked = false;
            this.forward = new AtomicReference[level];
            for (int i = 0; i < level; i++) {
                forward[i] = new AtomicReference<>(null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public LockFreeSkipList() {
        head = new Node<>(Integer.MIN_VALUE, null, MAX_LEVEL);
        tail = new Node<>(Integer.MAX_VALUE, null, MAX_LEVEL);
        for (int i = 0; i < MAX_LEVEL; i++) {
            head.forward[i].set(tail);
        }
    }

    private int randomLevel() {
        int level = 1;
        while (ThreadLocalRandom.current().nextDouble() < P && level < MAX_LEVEL) {
            level++;
        }
        return level;
    }

    @SuppressWarnings("unchecked")
    private boolean find(int key, Node<T>[] preds, Node<T>[] succs) {
        retry:
        while (true) {
            Node<T> curr = head;
            for (int level = MAX_LEVEL - 1; level >= 0; level--) {
                curr = head;
                Node<T> next = curr.forward[level].get();
                while (next != null && next.key < key) {
                    curr = next;
                    next = curr.forward[level].get();
                }
                while (next != null && next.marked) {
                    Node<T> succ = next.forward[level].get();
                    if (!curr.forward[level].compareAndSet(next, succ)) {
                        continue retry;
                    }
                    next = succ;
                }
                preds[level] = curr;
                succs[level] = next;
            }
            Node<T> succ = succs[0];
            if (succ != null && succ.marked) {
                continue retry;
            }
            return true;
        }
    }

    public T find(int key) {
        Node<T>[] preds = new Node[MAX_LEVEL];
        Node<T>[] succs = new Node[MAX_LEVEL];
        find(key, preds, succs);
        Node<T> succ = succs[0];
        if (succ != null && succ.key == key && !succ.marked) {
            return succ.value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean insert(int key, T value) {
        Node<T>[] preds = new Node[MAX_LEVEL];
        Node<T>[] succs = new Node[MAX_LEVEL];
        while (true) {
            if (!find(key, preds, succs)) {
                continue;
            }
            Node<T> succ = succs[0];
            // If key exists, update its value (upsert)
            if (succ != null && succ.key == key && !succ.marked) {
                succ.value = value; // Update value atomically
                return true;
            }
            // Otherwise, insert new node
            int newLevel = randomLevel();
            Node<T> newNode = new Node<>(key, value, newLevel);
            for (int level = 0; level < newLevel; level++) {
                newNode.forward[level].set(succs[level]);
            }
            if (!preds[0].forward[0].compareAndSet(succs[0], newNode)) {
                continue;
            }
            for (int level = 1; level < newLevel; level++) {
                while (true) {
                    if (preds[level].forward[level].compareAndSet(succs[level], newNode)) {
                        break;
                    }
                    if (!find(key, preds, succs)) {
                        break;
                    }
                    for (int i = level; i < newLevel; i++) {
                        newNode.forward[i].set(succs[i]);
                    }
                }
            }
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean delete(int key) {
        Node<T>[] preds = new Node[MAX_LEVEL];
        Node<T>[] succs = new Node[MAX_LEVEL];
        while (true) {
            if (!find(key, preds, succs)) {
                continue;
            }
            Node<T> succ = succs[0];
            if (succ == null || succ.key != key || succ.marked) {
                return false;
            }
            succ.marked = true;
            boolean success = true;
            for (int level = 0; level < succ.level; level++) {
                Node<T> next = succ.forward[level].get();
                if (next == null) {
                    continue;
                }
                if (!preds[level].forward[level].compareAndSet(succ, next)) {
                    success = false;
                    break;
                }
            }
            if (success) {
                return true;
            }
        }
    }

    // Print skiplist for debugging
    public void printSkiplist() {
        for (int level = MAX_LEVEL - 1; level >= 0; level--) {
            System.out.print("Level " + level + ": head --> ");
            Node<T> curr = head.forward[level].get();
            while (curr != tail) {
                System.out.print("[" + curr.key + "," + curr.value + "] --> ");
                curr = curr.forward[level].get();
            }
            System.out.println("tail");
        }
    }

    public static void main(String[] args) {
        LockFreeSkipList<String> skipList = new LockFreeSkipList<>();
        try {
            skipList.insert(1, "One");
            System.out.println("After insert(1, One):");
            skipList.printSkiplist();
            skipList.insert(2, "Two");
            System.out.println("After insert(2, Two):");
            skipList.printSkiplist();
            System.out.println("Find 2: " + skipList.find(2));
            skipList.insert(1, "OneAgain");
            System.out.println("After insert(1, OneAgain):");
            skipList.printSkiplist();
            System.out.println("Delete 2: " + skipList.delete(2));
            System.out.println("After delete(2):");
            skipList.printSkiplist();
            System.out.println("Find 2: " + skipList.find(2));
            System.out.println("Insert 2: " + skipList.insert(2, "TwoAgain"));
            System.out.println("After insert(2, TwoAgain):");
            skipList.printSkiplist();
            System.out.println("Find 2: " + skipList.find(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}