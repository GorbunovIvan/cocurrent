package org.example.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ReentrantLock1 implements Lock {

    Thread thread;

    @Override
    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void lockInterruptibly() throws InterruptedException {
        if (thread == Thread.currentThread())
            return;
        while (thread != null)
            wait();
        thread = Thread.currentThread();
    }

    @Override
    public boolean tryLock() {
        if (thread == Thread.currentThread())
            return false;
        if (thread == null) {
            thread = Thread.currentThread();
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (thread == Thread.currentThread())
            return false;
        if (thread != null)
            unit.sleep(time);
        if (thread == null) {
            thread = Thread.currentThread();
            return true;
        }
        return false;
    }

    @Override
    public synchronized void unlock() {
        if (thread != Thread.currentThread())
            throw new IllegalMonitorStateException("this thread is not owner of the lock");
        thread = null;
        notifyAll();
    }

    @Override
    public Condition newCondition() {
        return new ConditionObject(this);
    }

    static class ConditionObject implements Condition {
        Lock lock;
        public ConditionObject(Lock lock) {
            this.lock = lock;
        }
        @Override
        public void await() throws InterruptedException {
            lock.wait();
        }
        @Override
        public void awaitUninterruptibly() {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            long start = System.currentTimeMillis();
            lock.wait(nanosTimeout);
            return nanosTimeout - (System.currentTimeMillis() - start);
        }
        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toMillis(time);
            return awaitNanos(nanosTimeout) > 0;
        }
        @Override
        public boolean awaitUntil(Date deadline) throws InterruptedException {
            long nanosTimeout = deadline.getTime() - System.currentTimeMillis();
            return awaitNanos(nanosTimeout) > 0;
        }
        @Override
        public void signal() {
            lock.notify();
        }
        @Override
        public void signalAll() {
            lock.notifyAll();
        }
    }
}
