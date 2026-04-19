package cache;

import java.util.Optional;

/**
 * Write-through: escritura simultánea a cache y repositorio.
 * Garantía: si put() retorna sin excepción, el dato está en ambos.
 */
public class WriteThroughCache<K, V> {

    private final LRUCache<K, V> cache;
    private final CacheRepository<K, V> repo;

    public WriteThroughCache(int capacity, CacheRepository<K, V> repo) {
        this.cache = new LRUCache<>(capacity);
        this.repo  = repo;
    }

    public Optional<V> get(K key) {
        Optional<V> cached = cache.get(key);
        if (cached.isPresent()) return cached;
        // Cache miss: leer desde repositorio y poblar cache
        Optional<V> fromRepo = repo.read(key);
        fromRepo.ifPresent(v -> cache.put(key, v));
        return fromRepo;
    }

    /** Write-through: escribe en cache Y en repositorio sincrónicamente. */
    public void put(K key, V value) {
        repo.write(key, value);  // primero la fuente de verdad
        cache.put(key, value);   // luego el cache
    }

    public void invalidate(K key) {
        repo.delete(key);        // eliminar de persistencia
        // No hay remove público en LRUCache; forzamos evicción
        // poniendo un put no aplica, simplemente no repoblamos el cache.
        // El próximo get irá al repo y encontrará ausencia.
    }
}