package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class Unique4jAppInstance implements Unique4j.InstanceSelector {

    private final ImmutableConfig config;

    private FirstInstance firstInstanceHandler;
    private OtherInstance otherInstanceHandler;

    public Unique4jAppInstance(Unique4jConfig config) {
        this.config = new ImmutableConfig(config);
    }

    @Override
    public Unique4jLock newLock(FirstInstance firstInstanceHandler, OtherInstance otherInstanceHandler) {
        return new Unique4jIpcLock(config, firstInstanceHandler, otherInstanceHandler);
    }

    @Override
    public void requestSingleInstance(Consumer<Unique4j.InstanceConfig> instanceConfig)
            throws IOException, ExecutionException {
        final AtomicReference<UncheckedFunction<Unique4j.FirstInstanceContext, UncheckedRunnable>> firstInstanceContextFunctionRef
                = new AtomicReference<>();
        final AtomicReference<UncheckedFunction<Unique4j.OtherInstanceContext, UncheckedRunnable>> otherInstanceContextFunctionRef
                = new AtomicReference<>();
        instanceConfig.accept(new Unique4j.InstanceConfig() {
            @Override
            public Unique4j.InstanceConfig firstInstance(UncheckedFunction<Unique4j.FirstInstanceContext, UncheckedRunnable> ctx) {
                firstInstanceContextFunctionRef.set(ctx);
                return this;
            }

            @Override
            public Unique4j.InstanceConfig otherInstances(UncheckedFunction<Unique4j.OtherInstanceContext, UncheckedRunnable> ctx) {
                otherInstanceContextFunctionRef.set(ctx);
                return this;
            }
        });

        final UncheckedFunction<Unique4j.FirstInstanceContext, UncheckedRunnable> firstInstanceContextFunction =
                firstInstanceContextFunctionRef.get();
        final UncheckedFunction<Unique4j.OtherInstanceContext, UncheckedRunnable> otherInstanceContextFunction =
                otherInstanceContextFunctionRef.get();
        doWork(
                firstInstanceContextFunction != null ? ctx -> {
                    final UncheckedRunnable runnable = firstInstanceContextFunction.applyUnchecked(ctx);
                    return runnable != null ? () -> {
                        runnable.runUnchecked();
                        return null;
                    } : null;
                } : null,
                otherInstanceContextFunction != null ? ctx -> {
                    final UncheckedRunnable runnable = otherInstanceContextFunction.applyUnchecked(ctx);
                    return runnable != null ? () -> {
                        runnable.runUnchecked();
                        return null;
                    } : null;
                } : null
        );
    }

    @Override
    @SuppressWarnings("Convert2Diamond") // intelliJ thinks the project uses Java9 :I
    public <T> T requestSingleInstanceThenReturn(Consumer<Unique4j.InstanceConfigReturning<T>> instanceConfig) throws IOException, ExecutionException {
        final AtomicReference<UncheckedFunction<Unique4j.FirstInstanceContext, Callable<T>>> firstInstanceContextFunctionRef
                = new AtomicReference<>();
        final AtomicReference<UncheckedFunction<Unique4j.OtherInstanceContext, Callable<T>>> otherInstanceContextFunctionRef
                = new AtomicReference<>();
        instanceConfig.accept(new Unique4j.InstanceConfigReturning<T>() {
            @Override
            public Unique4j.InstanceConfigReturning<T> firstInstance(UncheckedFunction<Unique4j.FirstInstanceContext, Callable<T>> ctx) {
                firstInstanceContextFunctionRef.set(ctx);
                return this;
            }

            @Override
            public Unique4j.InstanceConfigReturning<T> otherInstances(UncheckedFunction<Unique4j.OtherInstanceContext, Callable<T>> ctx) {
                otherInstanceContextFunctionRef.set(ctx);
                return this;
            }
        });

        return doWork(firstInstanceContextFunctionRef.get(), otherInstanceContextFunctionRef.get());
    }

    private <T> T doWork(
            UncheckedFunction<Unique4j.FirstInstanceContext, Callable<T>> firstInstanceContextFunction,
            UncheckedFunction<Unique4j.OtherInstanceContext, Callable<T>> otherInstanceContextFunction
    ) throws IOException, ExecutionException {
        final CompletableFuture<Callable<T>> otherInstanceRunFunction = new CompletableFuture<>();
        final EnqueueingFirstInstance enqueueingFirstInstance;
        final Unique4jLock lock = newLock(
                enqueueingFirstInstance = new EnqueueingFirstInstance(),
                client -> {
                    if(otherInstanceContextFunction == null)
                        return;

                    try {
                        otherInstanceRunFunction.complete(otherInstanceContextFunction.applyUnchecked(new OtherInstanceContextImpl()));
                    } catch (Throwable t) {
                        otherInstanceRunFunction.completeExceptionally(t);
                    }

                    if(otherInstanceHandler != null)
                        otherInstanceHandler.onFirstInstanceFound(client);
                });

        final boolean locked = lock.tryLock();
        if(locked) {
            try {
                if (firstInstanceContextFunction != null) {
                    Callable<T> firstInstanceRunFunction = firstInstanceContextFunction.applyUnchecked(new FirstInstanceContextImpl());
                    enqueueingFirstInstance.setActualFirstInstance(firstInstanceHandler);

                    if (firstInstanceRunFunction != null)
                        return firstInstanceRunFunction.call();
                }
            } catch (Exception ex) {
                throw new ExecutionException("Failed to execute single instance function", ex);
            } finally {
                lock.unlock();
            }
        } else {
            try {
                final Callable<T> otherInstanceRunnable = otherInstanceRunFunction.get();
                if (otherInstanceRunnable != null)
                    return otherInstanceRunnable.call();
            } catch (ExecutionException ex) {
                throw new ExecutionException("Failed to execute other instances function", ex.getCause());
            } catch (Exception ex) {
                throw new ExecutionException("Failed to execute other instances function", ex);
            }
        }

        return null;
    }

    private class FirstInstanceContextImpl extends BaseContext implements Unique4j.FirstInstanceContext {
        @Override
        public Unique4j.FirstInstanceContext otherInstancesListener(FirstInstance instance) {
            firstInstanceHandler = instance;
            return this;
        }
    }

    private class OtherInstanceContextImpl extends BaseContext implements Unique4j.OtherInstanceContext {
        @Override
        public Unique4j.OtherInstanceContext firstInstanceListener(OtherInstance instance) {
            otherInstanceHandler = instance;
            return this;
        }
    }
}
