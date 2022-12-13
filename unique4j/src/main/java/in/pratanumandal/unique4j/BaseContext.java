package in.pratanumandal.unique4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

class BaseContext implements Unique4j.Context {

    @Override
    public Runnable waitForEvent(Consumer<Runnable> unlockInstance) {
        final CompletableFuture<Void> eventFuture = new CompletableFuture<>();
        unlockInstance.accept(() -> eventFuture.complete(null));
        return () -> {
            try {
                eventFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to wait for event", e);
            }
        };
    }

    @Override
    public Runnable doNothing() {
        return null;
    }

    @Override
    public Runnable thenExit(int statusCode) {
        return () -> System.exit(statusCode);
    }
}
