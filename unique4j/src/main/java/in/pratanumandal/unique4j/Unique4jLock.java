package in.pratanumandal.unique4j;

import java.io.IOException;

public interface Unique4jLock {

    static Unique4jLock create(Unique4jConfig config,
                               FirstInstance firstInstanceHandler,
                               OtherInstance otherInstanceHandler) {
        return new Unique4jIpcLock(config, firstInstanceHandler, otherInstanceHandler);
    }

    /**
     * Acquires the lock only if it is free (so the app is the first instance)
     * at the time of invocation.
     *
     * Acquires the lock if it is available and returns with the value {@code true}, receiving messages from
     * subsequent instances until unlocked.
     * If the lock is not available then this method will send a message to the first instance
     * and return with the value {@code false}.
     *
     * <p>A typical usage idiom for this method would be:
     * <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // run app stuff here
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     */
    boolean tryLock() throws IOException;

    /**
     * Attempts to release this lock.
     *
     * <p>If the current app instance is the holder of this lock then the
     * lock is released.  If the current thread is not the holder of this
     * lock then {@link UnsupportedOperationException} is thrown.
     *
     * @throws UnsupportedOperationException if the current app instance
     *         does not hold this lock
     */
    void unlock() throws IOException;

    /**
     * Queries if this lock is held by the current app instance.
     *
     * @return {@code true} if current app instance holds this lock and
     *         {@code false} otherwise
     */
    boolean isHeldByCurrentAppInstance();
}
