package in.pratanumandal.unique4j;

import java.util.logging.Level;
import java.util.logging.Logger;

class JulUnexpectedExceptionHandler implements UnexpectedExceptionHandler {

    public static final JulUnexpectedExceptionHandler INSTANCE = new JulUnexpectedExceptionHandler();
    private static final Logger LOGGER = Logger.getLogger(Unique4jInstance.class.getName());

    private JulUnexpectedExceptionHandler() {
    }

    @Override
    public void unexpectedException(IpcServer server, IpcClient client, Throwable t) {
        LOGGER.log(Level.SEVERE, t, () -> "Unexpected Unique4j exception (server: " + server + ", client: " + client + ")");
    }
}
