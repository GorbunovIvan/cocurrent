package org.example.synchronizers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

// Не доделано, запутался и оставил
public class Semaphore1 {

    private int permits;
    private int initPermits;
    private final Queue<Thread> waitingThreads = new LinkedList<>();

    public Semaphore1(int permits) {
        this.initPermits = permits;
        this.permits = permits;
    }

    public void acquire() throws InterruptedException {
        acquire(1);
    }

    public void acquireUninterruptibly() {
        acquireUninterruptibly(1);
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    public void release() {
        release(1);
    }

    public synchronized void acquire(int permits) throws InterruptedException {
        if (permits < 0)
            throw new IllegalArgumentException();
        if (this.permits <= 0)
            waitingThreads.offer(Thread.currentThread());
        while (this.permits <= 0)
            wait();
        this.permits -= permits;
        waitingThreads.remove(Thread.currentThread());
    }

    public void acquireUninterruptibly(int permits) {
        if (permits < 0)
            throw new IllegalArgumentException();
        try {
            acquire(permits);
        } catch (InterruptedException e) {
            waitingThreads.remove(Thread.currentThread());
            throw new RuntimeException(e);
        }
    }

    public boolean tryAcquire(int permits) {
        if (permits < 0)
            throw new IllegalArgumentException();
        if (this.permits > 0) {
            this.permits -= permits;
            return true;
        }
        return false;
    }

    public synchronized boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (this.permits <= 0)
            wait(unit.toMillis(timeout));
        return tryAcquire(permits);
    }

    public synchronized void release(int permits) {
        if (permits < 0)
            throw new IllegalArgumentException();
        this.permits = Math.min(initPermits, this.permits + permits);
        notifyAll();
    }

    public int availablePermits() {
        return permits;
    }

    public int drainPermits() {
        int currentPermits = permits;
        if (currentPermits > 0)
            acquireUninterruptibly(permits);
        else if (currentPermits < 0)
            release(permits);
        return currentPermits;
    }

    protected void reducePermits(int reduction) {
        if (reduction > initPermits || reduction < 0)
            throw new IllegalArgumentException();
        initPermits = initPermits - reduction;
        permits = Math.min(permits, initPermits);
    }

    public final boolean hasQueuedThreads() {
        return !waitingThreads.isEmpty();
    }

    public final int getQueueLength() {
        return waitingThreads.size();
    }

    protected Collection<Thread> getQueuedThreads() {
        return new LinkedList<>(waitingThreads);
    }

    public int getPermits() {
        return permits;
    }

    public String toString() {
        return super.toString() + "[Permits = " + getPermits() + "]";
    }

    private static final Object object = new Object();

    public static void main(String[] args) {
        var semaphore = new Semaphore1(5);

        List<String> timesOfThreads = new ArrayList<>(30);

        Runnable task = () -> {
            try {
                semaphore.acquire();
                timesOfThreads.add(getMessageAboutCurrentStateOfSemaphore(semaphore, "acquiring"));
                semaphore.release();
                timesOfThreads.add(getMessageAboutCurrentStateOfSemaphore(semaphore, "released"));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        List<Thread> threads = new ArrayList<>();
        IntStream.range(0, 15)
                        .forEach((i) -> threads.add(new Thread(task)));

        // running threads
        for (var thread : threads)
            thread.start();

        // waiting for threads to end
        try {
            for (var thread : threads)
                thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        timesOfThreads.sort(Comparator.naturalOrder());
        timesOfThreads.forEach(System.out::println);
    }

    private static String getMessageAboutCurrentStateOfSemaphore(Semaphore1 semaphore, String action) {
        return System.currentTimeMillis()
                + " - " + Thread.currentThread().getName()
                + " " + action
                + ", permits = " + semaphore.getPermits()
                + ", threads waiting - " + semaphore.getQueueLength();
    }
}
