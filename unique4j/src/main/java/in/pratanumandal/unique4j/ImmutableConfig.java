package in.pratanumandal.unique4j;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

class ImmutableConfig {

    private final String appId;
    private final File lockFile;
    private final IpcFactory ipcFactory;
    private final ExecutorService executorService;
    private final UnexpectedExceptionHandler exceptionHandler;

    public ImmutableConfig(Unique4jConfig config) {
        this.appId = Objects.requireNonNull(config.appId());
        this.lockFile = Objects.requireNonNull(config.lockFile());
        this.ipcFactory = Objects.requireNonNull(config.ipcFactory());
        this.executorService = Objects.requireNonNull(config.executorService());
        this.exceptionHandler = Objects.requireNonNull(config.exceptionHandler());
    }

    public String getAppId() {
        return appId;
    }

    public File getLockFile() {
        return lockFile;
    }

    public IpcFactory getIpcFactory() {
        return ipcFactory;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public UnexpectedExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }
}
