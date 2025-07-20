package org.avi.data.structures.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BPlusTree<K extends Comparable<K>, V> {

    private static final int ORDER = 4;

    abstract class Node {
        List<K> keys = new ArrayList<>();
        abstract boolean isLeaf();
    }

    class InternalNode extends Node {
        List<Node> children = new ArrayList<>();
        @Override
        boolean isLeaf() { return false; }
    }

    class LeafNode extends Node {
        List<V> values = new ArrayList<>();
        LeafNode next;
        @Override
        boolean isLeaf() { return true; }
    }

    private Node root = new LeafNode();

    // Public insert method
    public void insert(K key, V value) {
        Node newRoot = insert(root, key, value);
        if (newRoot != null) {
            InternalNode newInternal = new InternalNode();
            newInternal.keys.add(((InternalNode)newRoot).keys.get(0));
            newInternal.children.add(root);
            newInternal.children.add(((InternalNode)newRoot).children.get(1));
            root = newInternal;
        }
    }

    // Recursive insert
    private Node insert(Node node, K key, V value) {
        if (node.isLeaf()) {
            LeafNode leaf = (LeafNode) node;
            int index = Collections.binarySearch(leaf.keys, key);
            if (index >= 0) {
                leaf.values.set(index, value);
            } else {
                index = -index - 1;
                leaf.keys.add(index, key);
                leaf.values.add(index, value);
            }
            if (leaf.keys.size() > ORDER - 1) {
                return splitLeaf(leaf);
            }
        } else {
            InternalNode internal = (InternalNode) node;
            int index = Collections.binarySearch(internal.keys, key);
            index = index >= 0 ? index + 1 : -index - 1;
            Node child = internal.children.get(index);
            Node newChild = insert(child, key, value);
            if (newChild != null) {
                InternalNode newInternal = (InternalNode) newChild;
                internal.keys.add(index, newInternal.keys.get(0));
                internal.children.add(index + 1, newInternal.children.get(1));
                if (internal.keys.size() > ORDER - 1) {
                    return splitInternal(internal);
                }
            }
        }
        return null;
    }

    private Node splitLeaf(LeafNode leaf) {
        LeafNode newLeaf = new LeafNode();
        int mid = (ORDER + 1) / 2;
        newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        newLeaf.values.addAll(leaf.values.subList(mid, leaf.values.size()));
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        leaf.values.subList(mid, leaf.values.size()).clear();
        newLeaf.next = leaf.next;
        leaf.next = newLeaf;

        InternalNode parent = new InternalNode();
        parent.keys.add(newLeaf.keys.get(0));
        parent.children.add(leaf);
        parent.children.add(newLeaf);
        return parent;
    }

    private Node splitInternal(InternalNode node) {
        InternalNode newNode = new InternalNode();
        int mid = node.keys.size() / 2;
        K midKey = node.keys.get(mid);

        newNode.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        newNode.children.addAll(node.children.subList(mid + 1, node.children.size()));

        node.keys.subList(mid, node.keys.size()).clear();
        node.children.subList(mid + 1, node.children.size()).clear();

        InternalNode parent = new InternalNode();
        parent.keys.add(midKey);
        parent.children.add(node);
        parent.children.add(newNode);
        return parent;
    }

    // Search method
    public V search(K key) {
        Node node = root;
        while (!node.isLeaf()) {
            InternalNode internal = (InternalNode) node;
            int index = Collections.binarySearch(internal.keys, key);
            index = index >= 0 ? index + 1 : -index - 1;
            node = internal.children.get(index);
        }
        LeafNode leaf = (LeafNode) node;
        int index = Collections.binarySearch(leaf.keys, key);
        return index >= 0 ? leaf.values.get(index) : null;
    }

    // Optional: for debugging
    public void printTree() {
        printTree(root, 0);
    }

    private void printTree(Node node, int level) {
        String indent = " ".repeat(level * 4);
        System.out.print(indent + (node.isLeaf() ? "Leaf " : "Internal ") + node.keys + "\n");
        if (!node.isLeaf()) {
            for (Node child : ((InternalNode) node).children) {
                printTree(child, level + 1);
            }
        }
    }

    public static void main(String[] args) {
        BPlusTree<Integer, String> tree = new BPlusTree<>();
        tree.insert(10, "A");
        tree.insert(20, "B");
        tree.insert(5, "C");
        tree.insert(6, "D");
        tree.insert(15, "E");

        System.out.println("Search 15: " + tree.search(15)); // Should print "E"
        System.out.println("Search 7: " + tree.search(7));   // Should print null

        tree.printTree(); // Debug: print tree structure
    }
}
