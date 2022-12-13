package in.pratanumandal.unique4j;

import java.io.IOException;

public interface Unique4jLock {

    static Unique4jLock create(Unique4jConfig config,
                               FirstInstance firstInstanceHandler,
                               OtherInstance otherInstanceHandler) {
        return new Unique4jIpcLock(config, firstInstanceHandler, otherInstanceHandler);
    }

    boolean tryLock() throws IOException;

    void unlock() throws IOException;

    boolean isHeldByCurrentAppInstance();
}
