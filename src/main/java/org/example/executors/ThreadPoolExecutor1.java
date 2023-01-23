package org.example.executors;

import java.util.*;
import java.util.concurrent.*;

public class ThreadPoolExecutor1 implements ExecutorService {

    private final Map<Thread, Future<?>> threads;

    private final int corePoolSize;
    private final int maxPoolSize;

    private Thread daemon;
    private boolean isShutdown;

    public ThreadPoolExecutor1(int corePoolSize, int maxPoolSize) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        threads = new ConcurrentHashMap<>();
    }

    @Override
    public void execute(Runnable command) {
        runTask(new RunnableCallable<>(command, null));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return runTask(new RunnableCallable<>(null, task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return runTask(new RunnableCallable<T>(task, null, result));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return runTask(new RunnableCallable<>(task, null));
    }

    private <T> Future<T> runTask(RunnableCallable<T> task) {
        FutureImpl<T> future = new FutureImpl<>(null);
        if (isShutdown) {
            future.cancel(false);
            return future;
        }
        var thread = new Thread(() -> {
            try {
                T result = task.run();
                future.set(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        future.setThread(thread);
        threads.put(thread, future);
        try {
            executeThread(thread);
        } catch (InterruptedException e) {
            future.cancel(false);
        }
        if (isShutdown) {
            future.cancel(false);
            return future;
        }
        return future;
    }

    @Override
    public synchronized <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (var task : tasks)
            futures.add(submit(task));
        while (futures.stream().anyMatch(f -> !f.isDone() && !f.isCancelled()))
            wait(100);
        return futures;
    }

    @Override
    public synchronized <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (var task : tasks)
            futures.add(submit(task));
        long time = System.currentTimeMillis();
        while (futures.stream().anyMatch(f -> !f.isDone() && !f.isCancelled())) {
            wait(Math.min(100, unit.toMillis(timeout)));
            if (unit.toMillis(timeout) <= (System.currentTimeMillis() - time))
                break;
        }
        return futures;
    }

    @Override
    public synchronized <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (var task : tasks)
            futures.add(submit(task));
        while (true) {
            Optional<Future<T>> optionalFuture = futures.stream()
                    .filter(f -> f.isDone() || f.isCancelled())
                    .findAny();
            if (optionalFuture.isPresent())
                return optionalFuture.get().get();
            wait(100);
        }
    }

    @Override
    public synchronized <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (var task : tasks)
            futures.add(submit(task));
        long time = System.currentTimeMillis();
        while (true) {
            Optional<Future<T>> optionalFuture = futures.stream()
                    .filter(f -> f.isDone() || f.isCancelled())
                    .findAny();
            if (optionalFuture.isPresent())
                return optionalFuture.get().get();
            wait(Math.min(100, unit.toMillis(timeout)));
            if (unit.toMillis(timeout) <= (System.currentTimeMillis() - time))
                break;
        }
        return null;
    }

    private synchronized void executeThread(Thread thread) throws InterruptedException {
        if (isShutdown)
            return;
        if (threads.size() >= corePoolSize) {
            if (threads.size() < maxPoolSize)
                wait(5);
            while (threads.size() >= maxPoolSize && !isShutdown) {
                wait(10);
                if (daemon == null)
                    runDaemon();
            }
        }
        if (isShutdown)
            return;
        thread.start();
        if (daemon == null)
            runDaemon();
    }

    private void runDaemon() {
        final ThreadPoolExecutor1 exec = this;
        Runnable task = () -> {
            synchronized (exec) {
                threads.keySet().removeIf(thread -> !thread.isAlive());
                while (!exec.threads.isEmpty()) {
                    try {
                        exec.wait(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    threads.keySet().removeIf(thread -> !thread.isAlive());
                }
                exec.notifyAll();
                exec.daemon = null;
            }
        };
        daemon = new Thread(task);
        daemon.setDaemon(true);
        daemon.start();
    }

    @Override
    public synchronized void shutdown() {
        isShutdown = true;
        while (!isTerminated()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        notifyAll();
        if (daemon != null) {
            daemon.interrupt();
            daemon = null;
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> notExecutedThreads = new ArrayList<>();
        for (var entrySet : threads.entrySet()) {
            var thread = entrySet.getKey();
            notExecutedThreads.add(thread);
            thread.interrupt();
            var future = entrySet.getValue();
            future.cancel(true);
        }
        threads.clear();
        if (daemon != null) {
            daemon.interrupt();
            daemon = null;
        }
        return notExecutedThreads;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown && threads.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long timeToWait = unit.toMillis(timeout);
        long oneQuantum = timeToWait/100;
        int numberOfQuanta = 0;
        while (!threads.isEmpty() && numberOfQuanta < 100) {
            Thread.sleep(oneQuantum);
            numberOfQuanta++;
        }
        return threads.isEmpty();
    }

    static class RunnableCallable<V> {

        Runnable runnable;
        Callable<V> callable;
        V resultToReturnForRunnable;

        public RunnableCallable(Runnable runnable, Callable<V> callable) {
            this(runnable, callable, null);
        }

        public RunnableCallable(Runnable runnable, Callable<V> callable, V resultToReturnForRunnable) {
            this.runnable = runnable;
            this.callable = callable;
            this.resultToReturnForRunnable = resultToReturnForRunnable;
        }

        public V run() {
            if (runnable != null) {
                runnable.run();
                return resultToReturnForRunnable;
            } else {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
