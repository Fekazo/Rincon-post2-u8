package cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WriteThroughCacheTest {

    private Map<String, Integer> store;
    private CacheRepository<String, Integer> repo;
    private WriteThroughCache<String, Integer> cache;

    @BeforeEach
    void setUp() {
        store = new HashMap<>();
        repo = new CacheRepository<>() {
            @Override public void write(String k, Integer v) { store.put(k, v); }
            @Override public Optional<Integer> read(String k) { return Optional.ofNullable(store.get(k)); }
            @Override public void delete(String k) { store.remove(k); }
        };
        cache = new WriteThroughCache<>(2, repo);
    }

    @Test
    void putWritesToRepoAndCache() {
        cache.put("k", 42);
        assertEquals(Optional.of(42), repo.read("k"));
        assertEquals(Optional.of(42), cache.get("k"));
    }

    @Test
    void cacheMissAfterEvictionRecoverFromRepo() {
        cache.put("A", 1);
        cache.put("B", 2);
        // Cache lleno (capacity=2), put C evicta A
        cache.put("C", 3);

        // A sigue en repo
        assertEquals(Optional.of(1), repo.read("A"));

        // get("A") hace cache miss y recupera desde repo
        assertEquals(Optional.of(1), cache.get("A"));
    }
}