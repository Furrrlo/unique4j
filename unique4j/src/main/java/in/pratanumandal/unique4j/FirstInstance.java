package in.pratanumandal.unique4j;

import java.io.IOException;

@FunctionalInterface
public interface FirstInstance {

    /**
     * Method used in first instance to receive messages from subsequent instances.<br><br>
     *
     * @param otherInstanceClient IPC to receive messages from a subsequent client
     */
    void onOtherInstanceStarted(IpcClient otherInstanceClient) throws IOException, InterruptedException;
}
