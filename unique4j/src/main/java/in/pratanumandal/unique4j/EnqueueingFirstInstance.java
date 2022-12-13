package in.pratanumandal.unique4j;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class EnqueueingFirstInstance implements FirstInstance {

    private final Lock actualFirstInstanceLock = new ReentrantLock();
    private final Condition actualFirstInstanceSetCondition = actualFirstInstanceLock.newCondition();
    private volatile boolean isActualFirstInstanceSet;
    private volatile FirstInstance actualFirstInstance;

    @Override
    public void onOtherInstanceStarted(IpcClient otherInstanceClient) throws IOException, InterruptedException {
        if(!isActualFirstInstanceSet) {
            actualFirstInstanceLock.lock();
            try {
                while (!isActualFirstInstanceSet)
                    actualFirstInstanceSetCondition.await();
            } finally {
                actualFirstInstanceLock.unlock();
            }
        }

        if(actualFirstInstance != null)
            actualFirstInstance.onOtherInstanceStarted(otherInstanceClient);
    }

    void setActualFirstInstance(FirstInstance firstInstance) {
        actualFirstInstanceLock.lock();
        try {
            if(isActualFirstInstanceSet)
                throw new UnsupportedOperationException("FirstInstance was already set");

            actualFirstInstance = firstInstance;
            isActualFirstInstanceSet = true;
            actualFirstInstanceSetCondition.signalAll();
        } finally {
            actualFirstInstanceLock.unlock();
        }
    }
}
