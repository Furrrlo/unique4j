package in.pratanumandal.unique4j;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class MutableConfig implements Unique4jConfig {

    private String appId;
    private File lockFile;
    private IpcFactory ipcFactory;
    private ExecutorService executorService;
    private UnexpectedExceptionHandler exceptionHandler;

    @Override
    public String appId() {
        return appId;
    }

    @Override
    public Unique4jConfig appId(String appId) {
        this.appId = appId;
        return this;
    }

    @Override
    public File lockFile() {
        return lockFile;
    }

    @Override
    public Unique4jConfig lockFile(File lockFile) {
        this.lockFile = lockFile;
        return this;
    }

    @Override
    public IpcFactory ipcFactory() {
        return ipcFactory;
    }

    @Override
    public Unique4jConfig ipcFactory(IpcFactory ipcFactory) {
        this.ipcFactory = ipcFactory;
        return this;
    }

    @Override
    public ExecutorService executorService() {
        return executorService;
    }

    @Override
    public Unique4jConfig executorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    @Override
    public UnexpectedExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public Unique4jConfig exceptionHandler(UnexpectedExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }
}
