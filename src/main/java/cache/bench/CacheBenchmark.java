package cache.bench;

import cache.LRUCache;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class CacheBenchmark {

    @Param({"100", "1000", "10000"})
    private int cacheSize;

    private LRUCache<Integer, String> lruCache;
    private Map<Integer, String> directMap; // simula DB
    private Random rng;

    @Setup
    public void setup() {
        lruCache  = new LRUCache<>(cacheSize);
        directMap = new HashMap<>();
        rng       = new Random(42);
        // Poblar datos: 10× el tamaño del cache para simular misses
        for (int i = 0; i < cacheSize * 10; i++)
            directMap.put(i, "value-" + i);
    }

    @Benchmark
    public String withCache() {
        int key = rng.nextInt(cacheSize * 10);
        return lruCache.get(key).orElseGet(() -> {
            String v = directMap.get(key);
            lruCache.put(key, v);
            return v;
        });
    }

    @Benchmark
    public String withoutCache() {
        return directMap.get(rng.nextInt(cacheSize * 10));
    }
}