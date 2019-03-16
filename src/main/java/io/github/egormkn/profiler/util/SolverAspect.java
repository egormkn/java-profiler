package io.github.egormkn.profiler.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class SolverAspect {

    @Around("execution (* ru.ifmo.cycles.solver.Solver.solve(..))")
    public Object solve(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        System.out.format(">>> %s started%n", className);

        Object result = null;
        try {
            long startTime = System.currentTimeMillis();
            result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            System.err.format(">>> %s finished at %dms%n", className, endTime - startTime);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return result;
    }
}
