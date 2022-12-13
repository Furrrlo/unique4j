package in.pratanumandal.unique4j;

import java.io.IOException;

public interface Unique4jLock {

    boolean tryLock() throws IOException;

    void unlock() throws IOException;
}
