package cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU Cache thread-safe usando HashMap + lista doblemente enlazada.
 * get y put son O(1) amortizado.
 * Invariante: map.size() == list.size() <= capacity.
 */
public class LRUCache<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev, next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public Optional<V> get(K key) {
        lock.writeLock().lock(); // write lock: mueve el nodo
        try {
            Node<K, V> node = map.get(key);
            if (node == null) return Optional.empty();
            moveToFront(node);
            return Optional.of(node.value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            if (map.containsKey(key)) {
                Node<K, V> node = map.get(key);
                node.value = value;
                moveToFront(node);
            } else {
                if (map.size() == capacity) evict();
                Node<K, V> node = new Node<>(key, value);
                map.put(key, node);
                addToFront(node);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evict() {
        Node<K, V> lru = tail.prev;
        removeNode(lru);
        map.remove(lru.key);
    }

    private void moveToFront(Node<K, V> n) {
        removeNode(n);
        addToFront(n);
    }

    private void addToFront(Node<K, V> n) {
        n.next = head.next;
        n.prev = head;
        head.next.prev = n;
        head.next = n;
    }

    private void removeNode(Node<K, V> n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
    }

    public int size() {
        return map.size();
    }
}