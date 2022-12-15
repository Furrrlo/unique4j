package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

class Unique4jAppInstance implements Unique4j.InstanceSelector {

    private final ImmutableConfig config;

    private UncheckedFunction<Unique4j.FirstInstanceContext, UncheckedRunnable> firstInstanceContextFunction;
    private FirstInstance firstInstanceHandler;

    private UncheckedFunction<Unique4j.OtherInstanceContext, UncheckedRunnable> otherInstanceContextFunction;
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
        instanceConfig.accept(new Unique4j.InstanceConfig() {
            @Override
            public Unique4j.InstanceConfig firstInstance(UncheckedFunction<Unique4j.FirstInstanceContext, UncheckedRunnable> ctx) {
                firstInstanceContextFunction = ctx;
                return this;
            }

            @Override
            public Unique4j.InstanceConfig otherInstances(UncheckedFunction<Unique4j.OtherInstanceContext, UncheckedRunnable> ctx) {
                otherInstanceContextFunction = ctx;
                return this;
            }
        });

        final CompletableFuture<UncheckedRunnable> otherInstanceRunFunction = new CompletableFuture<>();
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
                    UncheckedRunnable firstInstanceRunFunction = firstInstanceContextFunction.applyUnchecked(new FirstInstanceContextImpl());
                    enqueueingFirstInstance.setActualFirstInstance(firstInstanceHandler);

                    if (firstInstanceRunFunction != null)
                        firstInstanceRunFunction.runUnchecked();
                }
            } catch (Exception ex) {
                throw new ExecutionException("Failed to execute single instance function", ex);
            } finally {
                lock.unlock();
            }
        } else {
            try {
                final UncheckedRunnable otherInstanceRunnable = otherInstanceRunFunction.get();
                if (otherInstanceRunnable != null)
                    otherInstanceRunnable.runUnchecked();
            } catch (ExecutionException ex) {
                throw new ExecutionException("Failed to execute other instances function", ex.getCause());
            } catch (Exception ex) {
                throw new ExecutionException("Failed to execute other instances function", ex);
            }
        }
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
