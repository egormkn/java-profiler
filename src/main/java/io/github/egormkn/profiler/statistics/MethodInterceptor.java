package io.github.egormkn.profiler.statistics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class MethodInterceptor {

    private final MethodLogger logger = MethodLogger.getInstance();

    @Pointcut("execution(* *(..)) && !within(io.github.egormkn.profiler..*)")
    public void profiledMethods() {
    }

    @Around("profiledMethods()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Signature signature = pjp.getSignature();
        logger.push(signature);
        try {
            return pjp.proceed();
        } finally {
            logger.pop(signature);
        }
    }
}