package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Unique4j {

    static InstanceSelector config(String appId, Function<Unique4jConfig, Unique4jConfig> makeConfig) {
        final Unique4jConfig config = makeConfig.apply(Unique4jConfig.createDefault(appId));
        return new Unique4jAppInstance(config);
    }

    static void requestSingleInstance(String appId, Consumer<InstanceConfig> instanceConfig) throws IOException {
        new Unique4jAppInstance(Unique4jConfig.createDefault(appId)).requestSingleInstance(instanceConfig);
    }

    interface InstanceSelector {

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
