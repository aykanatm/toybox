package com.github.murataykanat.toybox.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogExecutionTimeAspect {
    private static final Log _logger = LogFactory.getLog(LogExecutionTimeAspect.class);

    @Around("@annotation(com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime)")
    public Object logEntryExitExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        _logger.debug(joinPoint.getSignature().getName() + "() >>");
        long start = System.currentTimeMillis();

        Object proceed = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;


        _logger.debug("<< " + joinPoint.getSignature().getName() + "() [" + executionTime + " ms]");
        return proceed;
    }
}
