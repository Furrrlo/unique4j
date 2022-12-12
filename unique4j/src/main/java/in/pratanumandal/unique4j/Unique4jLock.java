package in.pratanumandal.unique4j;

public interface Unique4jLock {

    boolean tryLock();

    void unlock();
}
