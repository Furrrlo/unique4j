package in.pratanumandal.unique4j;

import java.io.IOException;

@FunctionalInterface
public interface OtherInstance {

    /** Method used in subsequent instances to send message to first instance */
    void onFirstInstanceFound(IpcClient firstInstanceClient) throws IOException;
}
