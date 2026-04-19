package cache;

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String msg) { super(msg); }
}