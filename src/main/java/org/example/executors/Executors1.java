package org.example.executors;

import java.util.concurrent.ExecutorService;

public class Executors1 {

    public static ExecutorService newSingleThreadExecutor() {
        return new ThreadPoolExecutor1(1, 1);
    }

    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor1(0, Integer.MAX_VALUE);
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor1(nThreads, nThreads);
    }
}
