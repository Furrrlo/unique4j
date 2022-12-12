package in.pratanumandal.unique4j;

@FunctionalInterface
public interface UnexpectedExceptionHandler {

    void unexpectedException(IpcServer server, IpcClient client, Throwable e);
}
