package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface Unique4j {

    static InstanceSelector withConfig(Unique4jConfig config) {
        return new Unique4jAppInstance(config);
    }

    static void requestSingleInstance(String appId, Consumer<InstanceConfig> instanceConfig)
            throws IOException, ExecutionException {
        new Unique4jAppInstance(Unique4jConfig.createDefault(appId)).requestSingleInstance(instanceConfig);
    }

    static <T> T requestSingleInstanceThenReturn(String appId, Consumer<InstanceConfigReturning<T>> instanceConfig)
            throws IOException, ExecutionException {
        return new Unique4jAppInstance(Unique4jConfig.createDefault(appId)).requestSingleInstanceThenReturn(instanceConfig);
    }

    static Unique4jLock newLock(String appId,
                                FirstInstance firstInstanceHandler,
                                OtherInstance otherInstanceHandler) {
        return new Unique4jIpcLock(Unique4jConfig.createDefault(appId), firstInstanceHandler, otherInstanceHandler);
    }

    interface InstanceSelector {

        Unique4jLock newLock(FirstInstance firstInstanceHandler, OtherInstance otherInstanceHandler);

        void requestSingleInstance(Consumer<InstanceConfig> instanceConfig) throws IOException, ExecutionException;

        <T> T requestSingleInstanceThenReturn(Consumer<InstanceConfigReturning<T>> instanceConfig) throws IOException, ExecutionException;
    }

    interface InstanceConfig {

        InstanceConfig firstInstance(UncheckedFunction<FirstInstanceContext, UncheckedRunnable> ctx);

        InstanceConfig otherInstances(UncheckedFunction<OtherInstanceContext, UncheckedRunnable> ctx);
    }

    interface InstanceConfigReturning<T> {

        InstanceConfigReturning<T> firstInstance(UncheckedFunction<FirstInstanceContext, Callable<T>> ctx);

        InstanceConfigReturning<T> otherInstances(UncheckedFunction<OtherInstanceContext, Callable<T>> ctx);
    }

    interface Context {

        UncheckedRunnable waitForEvent(Consumer<Runnable> unlockInstance);

        <T> Callable<T> waitForEventThenReturn(Consumer<Consumer<T>> unlockInstance);

        UncheckedRunnable doNothing();

        <T> Callable<T> doNothingThenReturn(T value);

        UncheckedRunnable thenExit(int statusCode);
    }

    interface FirstInstanceContext extends Context {

        FirstInstanceContext otherInstancesListener(FirstInstance instance);
    }

    interface OtherInstanceContext extends Context {

        OtherInstanceContext firstInstanceListener(OtherInstance instance);
    }
}
