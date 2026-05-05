package com.example.attendanceservice.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = DistributedLockRedisIntegrationTest.TestConfig.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        }
)
class DistributedLockRedisIntegrationTest {

    private static final String LOCK_ID = "redis-integration";
    private static final String REDIS_LOCK_KEY = "attendance-service:lock:integration:" + LOCK_ID;

    @Autowired
    private LockProbe lockProbe;

    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        redissonClient.getKeys().delete(REDIS_LOCK_KEY);
    }

    @AfterEach
    void tearDown() {
        redissonClient.getKeys().delete(REDIS_LOCK_KEY);
    }

    @Test
    void sameRedisLockKeySerializesConcurrentCalls() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        AtomicInteger concurrentExecutions = new AtomicInteger();
        AtomicInteger maxConcurrentExecutions = new AtomicInteger();

        try {
            Future<?> first = executorService.submit(() ->
                    lockProbe.enter(LOCK_ID, firstEntered, releaseFirst, concurrentExecutions, maxConcurrentExecutions)
            );

            assertThat(firstEntered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(redissonClient.getBucket(REDIS_LOCK_KEY).isExists()).isTrue();

            Future<?> second = executorService.submit(() ->
                    lockProbe.enter(LOCK_ID, secondEntered, releaseSecond, concurrentExecutions, maxConcurrentExecutions)
            );

            assertThat(secondEntered.await(300, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();

            assertThat(secondEntered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(maxConcurrentExecutions.get()).isEqualTo(1);

            releaseSecond.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            releaseSecond.countDown();
            executorService.shutdownNow();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        DistributedLockAspect distributedLockAspect(RedissonClient redissonClient) {
            return new DistributedLockAspect(redissonClient);
        }

        @Bean
        LockProbe lockProbe() {
            return new LockProbe();
        }
    }

    static class LockProbe {

        @DistributedLock(
                key = "'integration:' + #p0",
                waitTime = 2L,
                leaseTime = 5L
        )
        public void enter(
                String lockId,
                CountDownLatch entered,
                CountDownLatch release,
                AtomicInteger concurrentExecutions,
                AtomicInteger maxConcurrentExecutions
        ) {
            int active = concurrentExecutions.incrementAndGet();
            maxConcurrentExecutions.updateAndGet(current -> Math.max(current, active));
            entered.countDown();

            try {
                if (!release.await(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for test release latch");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } finally {
                concurrentExecutions.decrementAndGet();
            }
        }
    }
}
