package in.pratanumandal.unique4j;

import java.io.IOException;

@FunctionalInterface
public interface OtherInstance {

    void onFirstInstanceFound(IpcClient firstInstanceClient) throws IOException;
}
