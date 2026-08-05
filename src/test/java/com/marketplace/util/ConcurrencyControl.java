package com.marketplace.util;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrencyControl {
    /**
     *  Helper around `CountDownLatch.wait()` that catches InterruptedException and throws it as IllegalStateException,
     *  to improve concurrency control readability at call site.
     *  Additionally, fails on hanging test timeout.
     */
    public static void await(CountDownLatch latch)  {
        try {
            assertThat(latch.await(5, SECONDS))
                    .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
    /**
     *  Helper around `CyclicBarrier.await()` that catches exceptions and throws as IllegalStateException,
     *  to improve concurrency control readability at call site.
     *  Additionally, fails on hanging test timeout.
     */
    public static void await(CyclicBarrier barrier)  {
        try {
            barrier.await(5, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }
}
