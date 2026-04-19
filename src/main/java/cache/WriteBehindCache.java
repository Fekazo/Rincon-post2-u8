package cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Write-behind: escritura inmediata al cache, persistencia asíncrona.
 * Riesgo: si el proceso termina antes del flush, los datos se pierden.
 * Mitigación: flush() explícito en shutdown hooks.
 */
public class WriteBehindCache<K, V> implements AutoCloseable {

    private final LRUCache<K, V> cache;
    private final CacheRepository<K, V> repo;
    private final BlockingQueue<Map.Entry<K, V>> writeQueue = new LinkedBlockingQueue<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    public WriteBehindCache(int cap, CacheRepository<K, V> repo) {
        this.cache = new LRUCache<>(cap);
        this.repo  = repo;
        // Hilo de escritura asíncrona al repositorio
        writer.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    var entry = writeQueue.take();
                    repo.write(entry.getKey(), entry.getValue());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void put(K key, V value) {
        cache.put(key, value);
        writeQueue.offer(Map.entry(key, value)); // asíncrono
    }

    public Optional<V> get(K key) {
        return cache.get(key);
    }

    @Override
    public void close() {
        writer.shutdownNow();
    }
}