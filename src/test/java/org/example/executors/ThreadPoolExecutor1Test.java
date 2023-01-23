package org.example.executors;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolExecutor1Test {

    @Test
    void execute() {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService executor = Executors1.newFixedThreadPool(4);
        for (int i = 0; i < 3_000; i++)
            executor.execute(counter::incrementAndGet);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(3_000, counter.get());

        executor.shutdown();
    }

    @Test
    void submit() {
        ExecutorService executor = Executors1.newCachedThreadPool();

        // 1, 2
        Future<Integer> future1 = executor.submit(() -> 2 + 3);
        Future<Integer> future2 = executor.submit(() -> 5 - 2);

        try {
            assertEquals(5, future1.get());
            assertEquals(3, future2.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        // 3
        Future<Integer> future3 = executor.submit(() ->  {
            try {
                Thread.sleep(1000);
                return 1;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(future3.isDone());

        try {
            Thread.sleep(1000);
            assertEquals(1, future3.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        assertTrue(future3.isDone());

        // 4
        Future<Integer> future4 = executor.submit(() ->  {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 5);

        try {
            assertEquals(5, future4.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();
    }

    @Test
    void testInvokeAll() {
        var counter = new AtomicInteger(0);

        ExecutorService executor = Executors1.newFixedThreadPool(4);
        List<Callable<Integer>> tasks = new ArrayList<>(3_000);
        for (int i = 0; i < 3_000; i++)
            tasks.add(counter::incrementAndGet);

        try {
            List<Future<Integer>> futures = executor.invokeAll(tasks);
            assertEquals(3_000, counter.get());
            assertEquals(3_000, futures.size());
            int sum = 0;
            int sumExpected = 0;
            for (int i = 0; i < 3_000; i++) {
                assertTrue(futures.get(i).isDone());
                sum += futures.get(i).get();
                sumExpected += i+1;
            }
            assertEquals(sumExpected, sum);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();

        // with timeout
        var counter2 = new AtomicInteger(0);
        executor = Executors1.newFixedThreadPool(4);
        tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> {
                Thread.sleep(1000);
                return counter2.incrementAndGet();
            });
        }

        try {
            List<Future<Integer>> futures = executor.invokeAll(tasks, 2, TimeUnit.MILLISECONDS);
            assertTrue(counter2.get() < 10);
            assertEquals(10, futures.size());
            assertFalse(futures.get(futures.size()-1).isDone());
            int sum = 0;
            int sumExpected = 0;
            for (int i = 0; i < 10; i++) {
                if (futures.get(i).isDone())
                    sum += futures.get(i).get();
                sumExpected += i+1;
            }
            assertTrue(sum < sumExpected);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
    }

    @Test
    void testInvokeAny() {
        var counter = new AtomicInteger(2);

        ExecutorService executor = Executors1.newFixedThreadPool(4);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            tasks.add(() -> {
                Thread.sleep(500);
                return counter.incrementAndGet();
            });

        try {
            int anyResult = executor.invokeAny(tasks);
            assertTrue(anyResult > 2 && anyResult <= 10);
            Thread.sleep(1000);
            assertEquals(12, counter.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();

        // with timeout
        var counter2 = new AtomicInteger(2);

        executor = Executors1.newFixedThreadPool(4);
        tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            tasks.add(() -> {
                Thread.sleep(500);
                return counter2.incrementAndGet();
            });

        try {
            Integer anyResult = executor.invokeAny(tasks, 2, TimeUnit.MILLISECONDS);
            assertNull(anyResult);
            Thread.sleep(1000);
            assertEquals(12, counter2.get());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();
    }

    @Test
    void shutdown() {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService executor = Executors1.newSingleThreadExecutor();
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                Thread.sleep(10);
                return counter.incrementAndGet();
            });
        }
        executor.shutdown();

        assertTrue(counter.get() < 10);
    }

    @Test
    void shutdownNow() {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService executor = Executors1.newSingleThreadExecutor();
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Future<Integer> future = executor.submit(() -> {
                Thread.sleep(100);
                return counter.incrementAndGet();
            });
            futures.add(future);
        }
        executor.shutdownNow();

        assertTrue(counter.get() < 100);
        assertFalse(futures.get(futures.size()-1).isDone());
    }

    @Test
    void isShutdown() {
        ExecutorService executor = Executors1.newFixedThreadPool(4);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                Thread.sleep(100);
                return 1;
            });
        }
        assertFalse(executor.isShutdown());
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }

    @Test
    void isTerminated() {
        ExecutorService executor = Executors1.newFixedThreadPool(4);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                Thread.sleep(100);
                return 1;
            });
        }
        assertFalse(executor.isTerminated());
        executor.shutdown();
        assertTrue(executor.isTerminated());
    }

    @Test
    void awaitTermination() {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService executor = Executors1.newFixedThreadPool(4);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                Thread.sleep(100);
                return counter.incrementAndGet();
            });
        }
        assertFalse(executor.isTerminated());
        assertTrue(counter.get() < 10);
        executor.shutdown();
        try {
            boolean resultOfWaiting = executor.awaitTermination(1, TimeUnit.SECONDS);
            assertTrue(resultOfWaiting);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(executor.isTerminated());
    }
}