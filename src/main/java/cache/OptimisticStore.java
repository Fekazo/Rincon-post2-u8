package cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén con control de concurrencia optimista basado en versiones.
 * Simula el comportamiento de ETag en APIs REST.
 * Invariante: version se incrementa en cada escritura exitosa.
 */
public class OptimisticStore<K, V> {

    public record Versioned<V>(V value, long version) {}

    private final ConcurrentHashMap<K, Versioned<V>> store = new ConcurrentHashMap<>();

    /** Leer valor y su versión actual. */
    public Optional<Versioned<V>> get(K key) {
        return Optional.ofNullable(store.get(key));
    }

    /** Escritura sin condición (primera vez). */
    public Versioned<V> put(K key, V value) {
        var versioned = new Versioned<>(value, 1L);
        store.put(key, versioned);
        return versioned;
    }

    /**
     * Escritura condicional: solo si la versión actual == expectedVersion.
     * Simula If-Match de HTTP.
     * @throws OptimisticLockException si la versión no coincide.
     */
    public Versioned<V> updateIfMatch(K key, V newValue, long expectedVersion) {
        return store.compute(key, (k, current) -> {
            if (current == null)
                throw new IllegalStateException("Key not found: " + k);
            if (current.version() != expectedVersion)
                throw new OptimisticLockException(
                    "Expected v" + expectedVersion +
                    " but found v" + current.version());
            return new Versioned<>(newValue, current.version() + 1);
        });
    }
}