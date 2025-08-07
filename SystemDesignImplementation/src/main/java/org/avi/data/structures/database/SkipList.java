package org.avi.data.structures.database;

import java.util.Random;

public class SkipList<K extends Comparable<K>, V> {
    private static final int MAX_LEVEL = 16;
    private final Node<K, V> head = new Node<>(null, null, MAX_LEVEL);
    private int level = 0;
    private final Random random = new Random();

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V>[] forward;

        @SuppressWarnings("unchecked")
        public Node(K key, V value, int level) {
            this.key = key;
            this.value = value;
            this.forward = new Node[level];
        }
    }

    private int randomLevel() {
        int lvl = 1;
        while (random.nextBoolean() && lvl < MAX_LEVEL) {
            lvl++;
        }
        return lvl;
    }

    public V search(K key) {
        Node<K, V> x = head;
        for (int i = level - 1; i >= 0; i--) {
            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
                x = x.forward[i];
            }
        }
        x = x.forward[0];
        if (x != null && x.key.equals(key)) {
            return x.value;
        }
        return null;
    }

    public void insert(K key, V value) {
        Node<K, V>[] update = new Node[MAX_LEVEL];
        Node<K, V> x = head;

        for (int i = level - 1; i >= 0; i--) {
            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
                x = x.forward[i];
            }
            update[i] = x;
        }

        x = x.forward[0];
        if (x != null && x.key.equals(key)) {
            x.value = value;  // Update value
        } else {
            int lvl = randomLevel();
            if (lvl > level) {
                for (int i = level; i < lvl; i++) {
                    update[i] = head;
                }
                level = lvl;
            }
            Node<K, V> newNode = new Node<>(key, value, lvl);
            for (int i = 0; i < lvl; i++) {
                newNode.forward[i] = update[i].forward[i];
                update[i].forward[i] = newNode;
            }
        }
    }

    public void delete(K key) {
        Node<K, V>[] update = new Node[MAX_LEVEL];
        Node<K, V> x = head;

        for (int i = level - 1; i >= 0; i--) {
            while (x.forward[i] != null && x.forward[i].key.compareTo(key) < 0) {
                x = x.forward[i];
            }
            update[i] = x;
        }

        x = x.forward[0];
        if (x != null && x.key.equals(key)) {
            for (int i = 0; i < level; i++) {
                if (update[i].forward[i] != x) break;
                update[i].forward[i] = x.forward[i];
            }
            while (level > 0 && head.forward[level - 1] == null) {
                level--;
            }
        }
    }

    public static void main(String[] args) {
        SkipList<Integer, String> skipList = new SkipList<>();
        skipList.insert(10, "A");
        skipList.insert(20, "B");
        skipList.insert(5, "C");
        skipList.insert(6, "D");
        skipList.insert(15, "E");
        System.out.println("Search 15: " + skipList.search(15));
        System.out.println("Search 7: " + skipList.search(7));
        skipList.delete(10);
        System.out.println("Search 10: " + skipList.search(10));
    }
}
