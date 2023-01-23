package org.example.locks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantLock1Test {

    private static final int NUMBER_Of_ITERATION = 30_000;

    @Test
    void lock() {
        Lock lock = new ReentrantLock1();

        // creating threads
        TestObject.Counter counter = new TestObject.Counter();
        List<Thread> threads = new ArrayList<>(NUMBER_Of_ITERATION);
        for (int i = 0; i < NUMBER_Of_ITERATION; i++)
            threads.add(new Thread(new TestObject(lock, counter)));

        runThreadsAndWaitTheirEnd(threads);

        assertEquals(NUMBER_Of_ITERATION, counter.getCounter());
    }

    @Test
    void lockInterruptibly() {
    }

    @Test
    void tryLock() {
    }

    @Test
    void testTryLock() {
    }

    @Test
    void unlock() {
    }

    @Test
    void newCondition() {
    }

    private void runThreadsAndWaitTheirEnd(List<Thread> threads) {
        // running threads
        for (var thread : threads)
            thread.start();

        // waiting until all the threads will end
        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

