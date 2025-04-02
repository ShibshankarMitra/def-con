package com.homedepot.supplychain.enterpriselabormanagement.aspects;

import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmSystemException;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.ElmBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Aspect
public class ExceptionAspect {

    /**
     * This is an interceptor class designed to handle Exceptions. The method intercept(<RuntimeException>) can be overloaded and hence different exceptions can have different handler logic to be invoked.
     */

    @AfterThrowing(pointcut = "execution(* com.homedepot.supplychain.enterpriselabormanagement..*(..))&& !@annotation(com.homedepot.supplychain.enterpriselabormanagement.annotations.NoIntercept))", throwing = "ex")
    public void intercept(Exception ex) {
        if (ex instanceof ElmSystemException e) {
            log.error("{}: {}, Source: {}", e.getClass().getSimpleName(), e.getMessage(), e.getSource(), e.getCause());
        } else if (ex instanceof ElmBusinessException e) {
            log.error("{}: {}", e.getClass().getSimpleName(), e.getMessage(), ex.getCause());
        } else {
            log.error("{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }
}