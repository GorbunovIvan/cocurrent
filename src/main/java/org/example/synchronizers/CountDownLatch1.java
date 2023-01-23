package org.example.synchronizers;

import java.util.concurrent.TimeUnit;

public class CountDownLatch1 {

    private int count;
    private final int initCount;

    public CountDownLatch1(int count) {
        if (count < 0)
            throw new IllegalArgumentException("count < 0");
        this.initCount = count;
        this.count = count;
    }

    public synchronized void await() throws InterruptedException {
        wait();
    }

    public synchronized boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        long time = System.currentTimeMillis();
        wait(unit.toMillis(timeout));
        return timeout > (System.currentTimeMillis() - time);
    }

    public synchronized void countDown() {
        if (count == 0) {
            count = initCount;
            return;
        }
        count--;
        if (count == 0)
            notifyAll();
    }

    public int getCount() {
        return count;
    }

    public String toString() {
        return super.toString() + "[Count = " + getCount() + "]";
    }
}
