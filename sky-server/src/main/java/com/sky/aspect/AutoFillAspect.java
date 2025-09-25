package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.ognl.OgnlException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.apache.ibatis.ognl.OgnlRuntime.setFieldValue;

/**
 * 自定义切面，实现自动填充功能
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     * 1. execution 锁定包
     * 2. annotation 锁定方法
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPoint() {}

    /**
     * 前置通知，在方法执行前进行数据填充
     * @param joinPoint
     */
    @Before("autoFillPoint()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行数据填充");
        // 获取被拦截的方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        //获取当前方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        Object object = args[0];
        if (object == null) {
            return;
        }

        switch (operationType) {
            case INSERT:
                // 填充创建时间、更新时间、创建人、更新人
                // 获取当前时间
                LocalDateTime now = LocalDateTime.now();
                // 填充创建时间和更新时间
                setField(object, "createTime", now);
                setField(object, "updateTime", now);
                // 填充创建人、更新人，通过当前线程获取
                setField(object, "createUser", BaseContext.getCurrentId());
                setField(object, "updateUser", BaseContext.getCurrentId());
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("字段填充失败：" + fieldName, e);
        }
    }
}
