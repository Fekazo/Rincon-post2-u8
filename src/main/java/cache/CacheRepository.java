package cache;

import java.util.Optional;

public interface CacheRepository<K, V> {
    void write(K key, V value);   // persistir cambio
    Optional<V> read(K key);      // leer desde persistencia
    void delete(K key);           // eliminar de persistencia
}