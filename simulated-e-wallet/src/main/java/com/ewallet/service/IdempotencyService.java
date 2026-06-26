package com.ewallet.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory idempotency service that prevents duplicate processing
 * of idempotent requests (e.g., fund transfers).
 *
 * How it works:
 * - The client sends an X-Idempotency-Key header (UUID) with the request.
 * - On first request: the key is cached along with the response result.
 * - On duplicate request within the TTL window: the cached result is
 *   returned immediately without mutating any balances.
 *
 * Thread-safety:
 * - Uses ConcurrentHashMap for lock-free concurrent reads/writes.
 * - A background scheduler runs every 60 seconds to purge expired entries,
 *   preventing unbounded memory growth.
 *
 * Limitations:
 * - This is an in-memory store — cached results are lost on application
 *   restart. For production with strict idempotency guarantees, replace
 *   with a Redis-backed implementation.
 */
@Service
@Slf4j
public class IdempotencyService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * @param ttlMs  Time-to-live for cached idempotency keys (default: 300000ms = 5 minutes)
     */
    public IdempotencyService(
            @Value("${idempotency.ttl-ms:300000}") long ttlMs) {
        this.ttlMs = ttlMs;
    }

    @PostConstruct
    public void init() {
        // Schedule periodic cleanup every 60 seconds
        scheduler.scheduleAtFixedRate(this::purgeExpiredEntries, 60, 60, TimeUnit.SECONDS);
        log.info("IdempotencyService initialised with TTL={}ms", ttlMs);
    }

    /**
     * Check if the given idempotency key already has a cached result.
     *
     * @return  The cached ApiResponse if found and still valid, null otherwise
     */
    public Object getCachedResult(String idempotencyKey) {
        CacheEntry entry = cache.get(idempotencyKey);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiryMs) {
            cache.remove(idempotencyKey);
            return null;
        }
        return entry.result;
    }

    /**
     * Cache the result of a successfully processed request.
     *
     * @param idempotencyKey  The unique key for this operation
     * @param result          The response to cache (returned on duplicate requests)
     */
    public void cacheResult(String idempotencyKey, Object result) {
        long expiry = System.currentTimeMillis() + ttlMs;
        cache.put(idempotencyKey, new CacheEntry(result, expiry));
        log.debug("Cached idempotency key: {} (expires at {})", idempotencyKey, expiry);
    }

    /**
     * Check whether a key already exists in the cache (regardless of expiry).
     * Used for quick duplicate detection before processing.
     */
    public boolean isDuplicate(String idempotencyKey) {
        CacheEntry entry = cache.get(idempotencyKey);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expiryMs) {
            cache.remove(idempotencyKey);
            return false;
        }
        return true;
    }

    /**
     * Remove all expired entries from the cache to prevent memory leaks.
     */
    private void purgeExpiredEntries() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (now > entry.getValue().expiryMs) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Purged {} expired idempotency keys", removed);
        }
    }

    /**
     * Internal wrapper that pairs a cached result with its expiration timestamp.
     */
    private record CacheEntry(Object result, long expiryMs) {}
}
