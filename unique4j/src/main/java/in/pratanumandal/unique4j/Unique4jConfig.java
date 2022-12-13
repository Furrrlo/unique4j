package in.pratanumandal.unique4j;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface Unique4jConfig {

    static Unique4jConfig createDefault(String appId) {
        return new MutableConfig()
                .appId(appId)
                .lockFolder(new File(System.getProperty("java.io.tmpdir") + File.separator + appId))
                .executorService(Executors.newFixedThreadPool(5))
                .ipcFactory(new DynamicPortSocketIpcFactory(InetAddress.getLoopbackAddress(), 3000))
                .exceptionHandler(JulUnexpectedExceptionHandler.INSTANCE);
    }

    /**
     * The appId must be as unique as possible.
     * Avoid generic names like "my_app_id" or "hello_world".<br>
     * A good strategy is to use the entire package name (group ID + artifact ID) along with some random characters.
     *
     * @return Unique string representing the application ID
     */
    String appId();

    /**
     * The appId must be as unique as possible.
     * Avoid generic names like "my_app_id" or "hello_world".<br>
     * A good strategy is to use the entire package name (group ID + artifact ID) along with some random characters.
     *
     * @param appId Unique string representing the application ID
     */
    Unique4jConfig appId(String appId);

    File lockFolder();

    Unique4jConfig lockFolder(File lockFolder);

    IpcFactory ipcFactory();

    Unique4jConfig ipcFactory(IpcFactory ipcFactory);

    ExecutorService executorService();

    Unique4jConfig executorService(ExecutorService executorService);

    UnexpectedExceptionHandler exceptionHandler();

    Unique4jConfig exceptionHandler(UnexpectedExceptionHandler exceptionHandler);
}
