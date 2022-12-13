package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

class Unique4jInstanceImpl implements Unique4jInstance.InstanceSelector {

    private final ImmutableConfig config;

    private Function<Unique4jInstance.FirstInstanceContext, Runnable> firstInstanceContextFunction;
    private FirstInstance firstInstanceHandler;

    private Function<Unique4jInstance.OtherInstanceContext, Runnable> otherInstanceContextFunction;
    private OtherInstance otherInstanceHandler;
    private Runnable otherInstanceRunFunction;

    public Unique4jInstanceImpl(Unique4jConfig config) {
        this.config = new ImmutableConfig(config);
    }

    @Override
    public void requestSingleInstance(Consumer<Unique4jInstance.InstanceConfig> instanceConfig) throws IOException {
        instanceConfig.accept(new Unique4jInstance.InstanceConfig() {
            @Override
            public Unique4jInstance.InstanceConfig firstInstance(Function<Unique4jInstance.FirstInstanceContext, Runnable> ctx) {
                firstInstanceContextFunction = ctx;
                return this;
            }

            @Override
            public Unique4jInstance.InstanceConfig otherInstances(Function<Unique4jInstance.OtherInstanceContext, Runnable> ctx) {
                otherInstanceContextFunction = ctx;
                return this;
            }
        });

        final EnqueueingFirstInstance enqueueingFirstInstance;
        final Unique4jLock lock = new Unique4jIpcLock(
                config,
                enqueueingFirstInstance = new EnqueueingFirstInstance(),
                client -> {
                    if(otherInstanceContextFunction == null)
                        return;

                    otherInstanceRunFunction = otherInstanceContextFunction.apply(new OtherInstanceContextImpl());
                    if(otherInstanceHandler != null)
                        otherInstanceHandler.onFirstInstanceFound(client);
                });

        final boolean locked = lock.tryLock();
        if(locked) {
            try {
                if(firstInstanceContextFunction != null) {
                    Runnable firstInstanceRunFunction = firstInstanceContextFunction.apply(new FirstInstanceContextImpl());
                    enqueueingFirstInstance.setActualFirstInstance(firstInstanceHandler);

                    if (firstInstanceRunFunction != null)
                        firstInstanceRunFunction.run();
                }
            } finally {
                lock.unlock();
            }
        } else {
            if (otherInstanceRunFunction != null)
                otherInstanceRunFunction.run();
        }
    }

    private class FirstInstanceContextImpl extends BaseContext implements Unique4jInstance.FirstInstanceContext {
        @Override
        public Unique4jInstance.FirstInstanceContext otherInstancesListener(FirstInstance instance) {
            firstInstanceHandler = instance;
            return this;
        }
    }

    private class OtherInstanceContextImpl extends BaseContext implements Unique4jInstance.OtherInstanceContext {
        @Override
        public Unique4jInstance.OtherInstanceContext firstInstanceListener(OtherInstance instance) {
            otherInstanceHandler = instance;
            return this;
        }
    }
}
