package org.example.synchronizers;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Exchanger1<V> {

    V value1;
    V value2;

    Thread thread1;
    Thread thread2;

    boolean thread1IsAcquiringForExchange;
    boolean thread2IsAcquiringForExchange;

    int versionOfExchange;

    public Exchanger1() {}

    public V exchange(V x) throws InterruptedException {
        try {
            return exchange(x, 0L);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public V exchange(V x, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        return exchange(x, unit.toMillis(timeout));
    }

    private V exchange(V x, long timeoutMillis)
            throws InterruptedException, TimeoutException {
        Objects.requireNonNull(x);
        waitUntilBothThreadsWillBePrepared(timeoutMillis);
        if (Thread.currentThread() == thread1) {
            value1 = x;
            thread1IsAcquiringForExchange = true;
        } else if (Thread.currentThread() == thread2) {
            value2 = x;
            thread2IsAcquiringForExchange = true;
        }
        System.out.println(thread1 == null && thread2 == null ? "null" : Thread.currentThread() == thread1 ? "thread1" : "thread2" + "\n");
        int currentVersion = versionOfExchange;
        return exchangeValuesBetweenThreads(x, currentVersion);
    }

    private synchronized void waitUntilBothThreadsWillBePrepared(long timeoutMillis)
            throws InterruptedException, TimeoutException {
        if (thread1 != null && thread2 != null)
            return;
        if (thread1 == null && thread2 == null)
            thread1 = Thread.currentThread();
        else if (thread2 == null)
            thread2 = Thread.currentThread();
        else
            throw new IllegalStateException();
        notify();
        while (thread1 == null || thread2 == null) {
            long timeBefore = System.currentTimeMillis();
            wait(timeoutMillis);
            if (timeoutMillis != 0L)
                if (timeoutMillis <= System.currentTimeMillis() - timeBefore)
                    if (thread1 == null || thread2 == null)
                        throw new TimeoutException();
        }
    }

    private synchronized V exchangeValuesBetweenThreads(V value, int version)
            throws InterruptedException {
        // exchange already happened by another thread
        if (version < versionOfExchange) {
            if (Thread.currentThread() == thread1) {
                thread1IsAcquiringForExchange = false;
                return value1;
            } else {
                thread2IsAcquiringForExchange = false;
                return value2;
            }
        }
        while (!thread1IsAcquiringForExchange || !thread2IsAcquiringForExchange)
            wait();
        if (Thread.currentThread() != thread1
                && Thread.currentThread() != thread2)
            throw new IllegalStateException();
        versionOfExchange++;
        thread1IsAcquiringForExchange = false;
        thread2IsAcquiringForExchange = false;
        V receivedValue;
        if (Thread.currentThread() == thread1) {
            value1 = value2;
            value2 = value;
            receivedValue = value1;
        } else {
            value2 = value1;
            value1 = value;
            receivedValue = value2;
        }
        notify();
        return receivedValue;
    }

    public static void main(String[] args) {
        final var exchanger = new Exchanger1<Counter>();

        final var counter1 = new Exchanger1.Counter();
        final var counter2 = new Exchanger1.Counter();

        var thread1 = createThread(exchanger, counter1);
        var thread2 = createThread(exchanger, counter2);

        thread1.start();
        thread2.start();
    }

    static Thread createThread(Exchanger1<Counter> exchanger, Exchanger1.Counter counter) {
        return new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    Thread.sleep(new Random().nextInt(80) + 20); // from 20 to 100 millis
                    counter.incrementCounter();
                    var counterFromExchanger = exchanger.exchange(counter);
                    if (counterFromExchanger.getCounter() != counter.getCounter())
                        throw new IllegalStateException("actual is: " + counter.getCounter() + ", but received: " + counterFromExchanger.getCounter());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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
