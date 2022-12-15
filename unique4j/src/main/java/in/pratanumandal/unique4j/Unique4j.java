package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public interface Unique4j {

    static InstanceSelector withConfig(Unique4jConfig config) {
        return new Unique4jAppInstance(config);
    }

    static void requestSingleInstance(String appId, Consumer<InstanceConfig> instanceConfig)
            throws IOException, ExecutionException {
        new Unique4jAppInstance(Unique4jConfig.createDefault(appId)).requestSingleInstance(instanceConfig);
    }

    static Unique4jLock newLock(String appId,
                                FirstInstance firstInstanceHandler,
                                OtherInstance otherInstanceHandler) {
        return new Unique4jIpcLock(Unique4jConfig.createDefault(appId), firstInstanceHandler, otherInstanceHandler);
    }

    interface InstanceSelector {

        Unique4jLock newLock(FirstInstance firstInstanceHandler, OtherInstance otherInstanceHandler);

        void requestSingleInstance(Consumer<InstanceConfig> instanceConfig) throws IOException, ExecutionException;
    }

    interface InstanceConfig {

        InstanceConfig firstInstance(UncheckedFunction<FirstInstanceContext, UncheckedRunnable> ctx);

        InstanceConfig otherInstances(UncheckedFunction<OtherInstanceContext, UncheckedRunnable> ctx);
    }

    interface Context {

        UncheckedRunnable waitForEvent(Consumer<Runnable> unlockInstance);

        UncheckedRunnable doNothing();

        UncheckedRunnable thenExit(int statusCode);
    }

    interface FirstInstanceContext extends Context {

        FirstInstanceContext otherInstancesListener(FirstInstance instance);
    }

    interface OtherInstanceContext extends Context {

        OtherInstanceContext firstInstanceListener(OtherInstance instance);
    }
}
