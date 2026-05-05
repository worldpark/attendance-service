package com.example.attendanceservice.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "attendance-service:lock:";

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.example.attendanceservice.aop.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        DistributedLock distributedLock = getDistributedLock(joinPoint);
        String lockKey = LOCK_PREFIX + parseKey(joinPoint, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!locked) {
                throw new IllegalStateException("Failed to acquire distributed lock: " + lockKey);
            }

            return joinPoint.proceed();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private DistributedLock getDistributedLock(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        if (distributedLock == null) {
            Method targetMethod = resolveTargetMethod(joinPoint, method);
            distributedLock = targetMethod.getAnnotation(DistributedLock.class);
        }
        if (distributedLock == null) {
            throw new IllegalStateException("DistributedLock annotation not found");
        }
        return distributedLock;
    }

    private Method resolveTargetMethod(ProceedingJoinPoint joinPoint, Method method) {
        try {
            return joinPoint.getTarget()
                    .getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            return method;
        }
    }

    private String parseKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            if (parameterNames != null && i < parameterNames.length) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Object key = expressionParser.parseExpression(keyExpression).getValue(context);
        if (key == null) {
            throw new IllegalArgumentException("Distributed lock key must not be null");
        }
        return key.toString();
    }
}
