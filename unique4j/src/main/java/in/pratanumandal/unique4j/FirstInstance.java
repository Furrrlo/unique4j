package in.pratanumandal.unique4j;

import java.io.IOException;

@FunctionalInterface
public interface FirstInstance {

    void onOtherInstanceStarted(IpcClient client) throws IOException, InterruptedException;
}
