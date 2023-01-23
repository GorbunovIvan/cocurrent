package org.example.locks;

import java.util.concurrent.locks.Lock;

class TestObject implements Runnable {

    private final Lock lock;
    private final Counter counter;
    private final boolean readOnly;

    public TestObject(Lock lock, Counter counter) {
        this(lock, counter, false);
    }

    public TestObject(Lock lock, Counter counter, boolean readOnly) {
        this.lock = lock;
        this.counter = counter;
        this.readOnly = readOnly;
    }

    @Override
    public void run() {
        lock.lock();
        if (!readOnly)
            counter.incrementCounter();
        lock.unlock();
    }

    static class Counter {
        private int counter;

        public int getCounter() {
            return counter;
        }

        public void incrementCounter() {
            this.counter++;
        }
    }
}
