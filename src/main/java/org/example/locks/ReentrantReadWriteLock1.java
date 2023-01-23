package org.example.locks;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class ReentrantReadWriteLock1
        implements ReadWriteLock {

    private final ReadLock readLock;
    private final WriteLock writeLock;

    public ReentrantReadWriteLock1() {
        this.readLock = new ReadLock();
        this.writeLock = new WriteLock();
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    private Set<Thread> getReadLockThreads() {
        return readLock.threads;
    }

    private Thread getWriteLockThread() {
        return writeLock.thread;
    }

    class ReadLock implements Lock {

        Set<Thread> threads = new HashSet<>();

        @Override
        public void lock() {
            try {
                lockInterruptibly();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            Thread currentThread = Thread.currentThread();
            if (getWriteLockThread() == currentThread
                || threads.contains(currentThread))
                return;
            synchronized (ReentrantReadWriteLock1.this) {
                while (getWriteLockThread() != null)
                    ReentrantReadWriteLock1.this.wait();
                threads.add(currentThread);
            }
        }

        @Override
        public boolean tryLock() {
            Thread currentThread = Thread.currentThread();
            if (getWriteLockThread() == currentThread
                    || threads.contains(currentThread))
                return false;
            if (getWriteLockThread() == null) {
                threads.add(currentThread);
                return true;
            }
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            Thread currentThread = Thread.currentThread();
            if (getWriteLockThread() == currentThread
                    || threads.contains(currentThread))
                return false;
            if (getWriteLockThread() != null)
                unit.sleep(time);
            if (getWriteLockThread() == null) {
                threads.add(currentThread);
                return true;
            }
            return false;
        }

        @Override
        public void unlock() {
            if (!threads.contains(Thread.currentThread()))
                throw new IllegalMonitorStateException("this thread has no read-locks");
            synchronized (ReentrantReadWriteLock1.this) {
                threads.remove(Thread.currentThread());
                ReentrantReadWriteLock1.this.notifyAll();
            }
        }

        @Override
        public Condition newCondition() {
            return new ConditionObject(this);
        }
    }

    class WriteLock implements Lock {

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
        public void lockInterruptibly() throws InterruptedException {
            if (thread == Thread.currentThread())
                return;
            synchronized (ReentrantReadWriteLock1.this) {
                while (thread != null || !getReadLockThreads().isEmpty())
                    ReentrantReadWriteLock1.this.wait();
                thread = Thread.currentThread();
            }
        }

        @Override
        public boolean tryLock() {
            if (thread == Thread.currentThread())
                return false;
            if (thread == null && getReadLockThreads().isEmpty()) {
                thread = Thread.currentThread();
                return true;
            }
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            if (thread == Thread.currentThread())
                return false;
            if (thread != null || !getReadLockThreads().isEmpty())
                unit.sleep(time);
            if (thread == null && getReadLockThreads().isEmpty()) {
                thread = Thread.currentThread();
                return true;
            }
            return false;
        }

        @Override
        public void unlock() {
            if (thread != Thread.currentThread())
                throw new IllegalMonitorStateException("this thread is not owner of the lock");
            synchronized (ReentrantReadWriteLock1.this) {
                thread = null;
                ReentrantReadWriteLock1.this.notifyAll();
            }
        }

        @Override
        public Condition newCondition() {
            return new ConditionObject(this);
        }
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
