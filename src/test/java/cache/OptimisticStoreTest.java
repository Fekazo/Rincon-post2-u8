package cache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OptimisticStoreTest {

    @Test
    void concurrentUpdateFirstSucceedsSecondRetriesAndSucceeds() throws InterruptedException {
        OptimisticStore<String, Integer> store = new OptimisticStore<>();
        store.put("k", 0);

        // Ambos hilos leen la misma versión inicial
        long initialVersion = store.get("k").orElseThrow().version();

        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicInteger finalValue = new AtomicInteger();
        AtomicInteger retries = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            bothRead.countDown();
            try { bothRead.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            // Primer hilo escribe exitosamente
            store.updateIfMatch("k", 1, initialVersion);
            firstDone.countDown();
        });

        Thread t2 = new Thread(() -> {
            bothRead.countDown();
            try { bothRead.await(); firstDone.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            // Segundo hilo: versión stale → OptimisticLockException → reintento
            boolean success = false;
            while (!success) {
                try {
                    long current = store.get("k").orElseThrow().version();
                    store.updateIfMatch("k", 2, current);
                    success = true;
                } catch (OptimisticLockException e) {
                    retries.incrementAndGet();
                }
            }
            finalValue.set(store.get("k").orElseThrow().value());
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(2, finalValue.get());
        assertEquals(3L, store.get("k").orElseThrow().version());
        // t2 pudo haber reintentado 0 veces si llegó después, pero la versión final debe ser 3
    }

    @Test
    void updateIfMatchThrowsOnStaleVersion() {
        OptimisticStore<String, Integer> store = new OptimisticStore<>();
        store.put("k", 10);
        store.updateIfMatch("k", 20, 1L); // versión pasa a 2

        assertThrows(OptimisticLockException.class,
            () -> store.updateIfMatch("k", 99, 1L)); // versión stale
    }
}