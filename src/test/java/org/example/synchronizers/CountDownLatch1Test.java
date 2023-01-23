package org.example.synchronizers;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CountDownLatch1Test {

    @Test
    void await() {
    }

    @Test
    void testAwait() {
    }

    @Test
    void countDown() {
        var countDownLatch = new CountDownLatch1(5);

        List<String> path = new ArrayList<>();
        Runnable task = () -> {
            countDownLatch.countDown();
            path.add("counted down");
            try {
                // will proceed only when all the other threads will count down
                while (countDownLatch.getCount() > 0)
                    countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            path.add("proceeding after releasing");
        };

        IntStream.range(0, 5)
                .forEach(i -> new Thread(task).start());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<String> pathExpected = List.of("counted down",
                                            "counted down",
                                            "counted down",
                                            "counted down",
                                            "counted down",
                                            "proceeding after releasing",
                                            "proceeding after releasing",
                                            "proceeding after releasing",
                                            "proceeding after releasing",
                                            "proceeding after releasing");

        assertEquals(pathExpected, path);
    }

    @Test
    void getCount() {
    }

    @Test
    void testToString() {
    }
}