package in.pratanumandal.unique4j;

@FunctionalInterface
public interface UnexpectedExceptionHandler {

    /**
     * Method to receive and handle exceptions occurring while first instance is listening for subsequent instances.<br><br>
     *
     * By default, logs all exceptions using JUL. Override this method to handle exceptions explicitly.<br><br>
     *
     * @param t exception occurring while first instance is listening for subsequent instances
     */
    void unexpectedException(IpcServer server, IpcClient client, Throwable t);
}
