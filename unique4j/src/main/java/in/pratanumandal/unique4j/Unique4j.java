package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Unique4j {

    static InstanceSelector withConfig(Unique4jConfig config) {
        return new Unique4jAppInstance(config);
    }

    static void requestSingleInstance(String appId, Consumer<InstanceConfig> instanceConfig) throws IOException {
        new Unique4jAppInstance(Unique4jConfig.createDefault(appId)).requestSingleInstance(instanceConfig);
    }

    static Unique4jLock newLock(String appId,
                                FirstInstance firstInstanceHandler,
                                OtherInstance otherInstanceHandler) {
        return new Unique4jIpcLock(Unique4jConfig.createDefault(appId), firstInstanceHandler, otherInstanceHandler);
    }

    interface InstanceSelector {

        Unique4jLock newLock(FirstInstance firstInstanceHandler, OtherInstance otherInstanceHandler);

        void requestSingleInstance(Consumer<InstanceConfig> instanceConfig) throws IOException;
    }

    interface InstanceConfig {

        InstanceConfig firstInstance(Function<FirstInstanceContext, Runnable> ctx);

        InstanceConfig otherInstances(Function<OtherInstanceContext, Runnable> ctx);
    }

    interface Context {

        Runnable waitForEvent(Consumer<Runnable> unlockInstance);

        Runnable doNothing();

        Runnable thenExit(int statusCode);
    }

    interface FirstInstanceContext extends Context {

        FirstInstanceContext otherInstancesListener(FirstInstance instance);
    }

    interface OtherInstanceContext extends Context {

        OtherInstanceContext firstInstanceListener(OtherInstance instance);
    }
}
