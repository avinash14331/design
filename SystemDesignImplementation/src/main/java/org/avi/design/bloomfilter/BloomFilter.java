package org.avi.design.bloomfilter;

import java.util.BitSet;

public class BloomFilter<T> {
    private final int bitSize;
    private final int numHashFunctions;
    private final BitSet bitSet;

    public BloomFilter(int bitSize, int numHashFunctions) {
        this.bitSize = bitSize;
        this.numHashFunctions = numHashFunctions;
        this.bitSet = new BitSet(bitSize);
    }

    // Add an element to the filter
    public void add(T item) {
        int[] hashes = getHashes(item);
        for (int hash : hashes) {
            bitSet.set(Math.abs(hash % bitSize), true);
        }
    }

    // Check if element might exist
    public boolean mightContain(T item) {
        int[] hashes = getHashes(item);
        for (int hash : hashes) {
            if (!bitSet.get(Math.abs(hash % bitSize))) {
                return false; // definitely not present
            }
        }
        return true; // possibly present
    }

    // Generate multiple hash values
    private int[] getHashes(T item) {
        int[] result = new int[numHashFunctions];
        int baseHash = item.hashCode();

        for (int i = 0; i < numHashFunctions; i++) {
            result[i] = baseHash * (i + 1) ^ (baseHash >>> i);
        }
        return result;
    }

    // For demo
    public static void main(String[] args) {
        BloomFilter<String> filter = new BloomFilter<>(100, 3);

        filter.add("dog");
        filter.add("cat");

        System.out.println("dog? " + filter.mightContain("dog")); // true
        System.out.println("cat? " + filter.mightContain("cat")); // true
        System.out.println("rat? " + filter.mightContain("rat")); // false
    }
}

