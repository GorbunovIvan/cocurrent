package org.example.executors;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureImpl<T> implements Future<T> {

    private T result;
    private Thread thread;
    private boolean isCancelled;
    private boolean isDone;

    public FutureImpl(T result) {
        this.result = result;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone)
            return false;
        isCancelled = true;
        if (thread == null)
            return false;
        if (!thread.isAlive())
            return false;
        if (mayInterruptIfRunning)
            thread.interrupt();
        notifyAll();
        return mayInterruptIfRunning;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!isDone && !isCancelled)
            wait();
        return result;
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException {
        while (!isDone && !isCancelled)
            wait(unit.toMillis(timeout));
        return result;
    }

    synchronized void set(T result) {
        this.result = result;
        isDone = true;
        notifyAll();
    }

    void setThread(Thread thread) {
        this.thread = thread;
    }
}
