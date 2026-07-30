package com.marketplace.util;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrencyControl {
    /**
     *  Helper around `CountDownLatch.wait()` that catches InterruptedException and throws it as RuntimeException,
     *  to improve concurrency control readability at call site.
     *  Additionally, timeouts hanging tests.
     */
    public static void await(CountDownLatch latch)  {
        try {
            assertThat(latch.await(5, SECONDS))
                    .isTrue();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
