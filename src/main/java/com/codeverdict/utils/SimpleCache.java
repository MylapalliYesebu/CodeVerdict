package com.codeverdict.utils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// THREAD-SAFETY: Accessed by multiple request threads.
// Using ReadWriteLock — allows concurrent reads while ensuring exclusive writes.
public class SimpleCache<K, V> {

    private final int maxSize;
    private final long ttlSeconds;
    private final LinkedHashMap<K, CacheEntry<V>> map;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SimpleCache(int maxSize, long ttlSeconds) {
        this.maxSize = maxSize;
        this.ttlSeconds = ttlSeconds;
        this.map = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > maxSize;
            }
        };
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            map.put(key, new CacheEntry<>(value, Instant.now().plusSeconds(ttlSeconds)));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<V> get(K key) {
        lock.readLock().lock();
        CacheEntry<V> entry;
        try {
            entry = map.get(key);
        } finally {
            lock.readLock().unlock();
        }

        if (entry == null) {
            return Optional.empty();
        }

        if (Instant.now().isAfter(entry.expiryTime)) {
            invalidate(key);
            return Optional.empty();
        }

        return Optional.of(entry.value);
    }

    public void invalidate(K key) {
        lock.writeLock().lock();
        try {
            map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static class CacheEntry<V> {
        final V value;
        final Instant expiryTime;

        CacheEntry(V value, Instant expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }
}
