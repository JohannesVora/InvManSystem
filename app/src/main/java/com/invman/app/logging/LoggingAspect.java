package com.invman.app.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "invman.logging.trace-enabled", havingValue = "true", matchIfMissing = false)
public class LoggingAspect {

    private static final int MAX_LENGTH = 200;

    @Pointcut(
        "within(com.invman.dataprocessing.controller..*) || " +
        "within(com.invman.dataprocessing.service..*) || " +
        "within(com.invman.outbound..*)"
    )
    public void applicationFlow() {}

    @Around("applicationFlow()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String cls    = sig.getDeclaringType().getSimpleName();
        String method = sig.getName();
        String args   = formatArgs(pjp.getArgs());

        log.debug("→ {}.{}({})", cls, method, args);
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.debug("← {}.{} returned {} in {}ms",
                    cls, method, formatReturn(result), System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.error("✗ {}.{} threw {} after {}ms: {}",
                    cls, method, t.getClass().getSimpleName(),
                    System.currentTimeMillis() - start, t.getMessage(), t);
            throw t;
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        return Arrays.stream(args).map(this::safe).collect(Collectors.joining(", "));
    }

    private String formatReturn(Object result) {
        return result == null ? "null" : safe(result);
    }

    private String safe(Object obj) {
        if (obj == null) return "null";
        try {
            String s = String.valueOf(obj);
            return s.length() <= MAX_LENGTH ? s : s.substring(0, MAX_LENGTH) + "…";
        } catch (Throwable t) {
            return "<toString() failed: " + t.getClass().getSimpleName() + ">";
        }
    }
}
