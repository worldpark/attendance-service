package com.example.attendanceservice.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistributedLockAspectTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock rLock = mock(RLock.class);
    private final DistributedLockAspect distributedLockAspect = new DistributedLockAspect(redissonClient);

    @Test
    void proceedsAfterAcquiringLockAndUnlocks() throws Throwable {
        UUID userId = UUID.fromString("018f5f8d-7a63-7000-8000-abcdef123456");
        LocalDateTime attendanceDate = LocalDateTime.parse("2026-05-05T09:00:00");
        TestTarget target = new TestTarget();
        ProceedingJoinPoint joinPoint = joinPoint(target, "lockedMethod", userId, attendanceDate);
        String lockKey = "attendance-service:lock:" + userId + ":" + attendanceDate;

        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(5L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = distributedLockAspect.lock(joinPoint);

        assertThat(result).isEqualTo("ok");
        var ordered = inOrder(redissonClient, rLock, joinPoint);
        ordered.verify(redissonClient).getLock(lockKey);
        ordered.verify(rLock).tryLock(5L, 10L, TimeUnit.SECONDS);
        ordered.verify(joinPoint).proceed();
        ordered.verify(rLock).unlock();
    }

    @Test
    void doesNotProceedWhenLockAcquisitionFails() throws Throwable {
        UUID userId = UUID.fromString("018f5f8d-7a63-7000-8000-abcdef123456");
        LocalDateTime attendanceDate = LocalDateTime.parse("2026-05-05T09:00:00");
        ProceedingJoinPoint joinPoint = joinPoint(new TestTarget(), "lockedMethod", userId, attendanceDate);
        String lockKey = "attendance-service:lock:" + userId + ":" + attendanceDate;

        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(5L, 10L, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> distributedLockAspect.lock(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(lockKey);

        verify(joinPoint, never()).proceed();
        verify(rLock, never()).unlock();
    }

    @Test
    void unlocksEvenWhenTargetThrowsException() throws Throwable {
        UUID userId = UUID.fromString("018f5f8d-7a63-7000-8000-abcdef123456");
        LocalDateTime attendanceDate = LocalDateTime.parse("2026-05-05T09:00:00");
        ProceedingJoinPoint joinPoint = joinPoint(new TestTarget(), "lockedMethod", userId, attendanceDate);
        String lockKey = "attendance-service:lock:" + userId + ":" + attendanceDate;

        when(redissonClient.getLock(lockKey)).thenReturn(rLock);
        when(rLock.tryLock(5L, 10L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("boom"));

        assertThatThrownBy(() -> distributedLockAspect.lock(joinPoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boom");

        verify(rLock).unlock();
    }

    private ProceedingJoinPoint joinPoint(Object target, String methodName, Object... args) throws NoSuchMethodException {
        Method method = target.getClass().getMethod(methodName, UUID.class, LocalDateTime.class);
        MethodSignature signature = mock(MethodSignature.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }

    static class TestTarget {

        @DistributedLock(
                key = "#p0 + ':' + #p1",
                waitTime = 5L,
                leaseTime = 10L
        )
        public String lockedMethod(UUID userId, LocalDateTime attendanceDate) {
            return "ok";
        }
    }
}
