package dev.argon.chunks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Bounded, thread-safe task queue suitable for scheduling pure data work.
 *
 * Larger priority values run first. Re-offering an existing key only replaces
 * its queued task when the new priority is higher. This class does not create
 * threads or execute tasks; Minecraft thread ownership remains the caller's
 * responsibility.
 */
public final class BoundedPriorityTaskQueue<K, V> {
    public enum OfferResult {
        ADDED,
        PRIORITY_RAISED,
        ALREADY_QUEUED,
        FULL
    }

    public record Task<K, V>(K key, V value, int priority) {
    }

    private static final class Entry<K, V> {
        private final K key;
        private final V value;
        private final int priority;
        private final long sequence;

        private Entry(K key, V value, int priority, long sequence) {
            this.key = key;
            this.value = value;
            this.priority = priority;
            this.sequence = sequence;
        }
    }

    private final int capacity;
    private final Map<K, Entry<K, V>> current = new HashMap<>();
    private final PriorityQueue<Entry<K, V>> queue = new PriorityQueue<>(
            Comparator.<Entry<K, V>>comparingInt(entry -> entry.priority)
                    .reversed()
                    .thenComparingLong(entry -> entry.sequence));
    private long nextSequence;

    public BoundedPriorityTaskQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized OfferResult offer(K key, V value, int priority) {
        if (key == null || value == null) {
            throw new NullPointerException("key and value must not be null");
        }

        Entry<K, V> existing = current.get(key);
        if (existing != null) {
            if (priority <= existing.priority) {
                return OfferResult.ALREADY_QUEUED;
            }
            Entry<K, V> upgraded = new Entry<>(key, value, priority, nextSequence++);
            current.put(key, upgraded);
            queue.add(upgraded);
            return OfferResult.PRIORITY_RAISED;
        }

        if (current.size() >= capacity) {
            return OfferResult.FULL;
        }

        Entry<K, V> entry = new Entry<>(key, value, priority, nextSequence++);
        current.put(key, entry);
        queue.add(entry);
        return OfferResult.ADDED;
    }

    public synchronized Optional<Task<K, V>> poll() {
        while (!queue.isEmpty()) {
            Entry<K, V> entry = queue.poll();
            if (current.get(entry.key) != entry) {
                continue; // Stale entry left behind by a priority upgrade or cancel.
            }
            current.remove(entry.key);
            return Optional.of(new Task<>(entry.key, entry.value, entry.priority));
        }
        return Optional.empty();
    }

    public synchronized boolean cancel(K key) {
        return current.remove(key) != null;
    }

    public synchronized boolean contains(K key) {
        return current.containsKey(key);
    }

    public synchronized int size() {
        return current.size();
    }

    public int capacity() {
        return capacity;
    }

    public synchronized boolean isEmpty() {
        return current.isEmpty();
    }

    public synchronized void clear() {
        current.clear();
        queue.clear();
    }
}
