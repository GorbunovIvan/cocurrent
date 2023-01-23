package org.example.locks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantReadWriteLock1Test {

    private static final int NUMBER_Of_ITERATION = 6_000;

    @Test
    void readLock() {
        ReadWriteLock lock = new ReentrantReadWriteLock1();

        Lock readLock = lock.readLock();
        Lock writeLock = lock.writeLock();

        // creating threads (for every write there will be 10 reads)
        TestObject.Counter counter = new TestObject.Counter();
        List<Thread> threads = new ArrayList<>(NUMBER_Of_ITERATION*5);
        for (int i = 0; i < NUMBER_Of_ITERATION; i++) {
            for (int r = 0; r < 4; r++)
                threads.add(new Thread(new TestObject(readLock, counter, true)));
            threads.add(new Thread(new TestObject(writeLock, counter, false)));
        }
        Collections.shuffle(threads);

        runThreadsAndWaitTheirEnd(threads);

        assertEquals(NUMBER_Of_ITERATION, counter.getCounter());
    }

    @Test
    void writeLock() {
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