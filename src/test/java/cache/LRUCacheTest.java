package cache;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {

    @Test
    void evictsLruAfterAccess() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);

        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        // Orden LRU->MRU: A, B, C

        cache.get("A");
        // Orden LRU->MRU: B, C, A

        cache.put("D", 4);
        // B es el LRU, debe ser evictado

        assertEquals(3, cache.size());
        assertFalse(cache.get("B").isPresent(), "B debió ser evictado");
        assertTrue(cache.get("A").isPresent());
        assertTrue(cache.get("C").isPresent());
        assertTrue(cache.get("D").isPresent());
    }
}