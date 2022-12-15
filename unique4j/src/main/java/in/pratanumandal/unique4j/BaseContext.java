package in.pratanumandal.unique4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class BaseContext implements Unique4j.Context {

    @Override
    public UncheckedRunnable waitForEvent(Consumer<Runnable> unlockInstance) {
        final CompletableFuture<Void> eventFuture = new CompletableFuture<>();
        unlockInstance.accept(() -> eventFuture.complete(null));
        return eventFuture::get;
    }

    @Override
    public UncheckedRunnable doNothing() {
        return null;
    }

    @Override
    public UncheckedRunnable thenExit(int statusCode) {
        return () -> System.exit(statusCode);
    }
}
