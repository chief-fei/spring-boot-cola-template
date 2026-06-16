package __PACKAGE_NAME__.infrastructure.aop;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("@annotation(__PACKAGE_NAME__.client.annotation.Log)")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        __PACKAGE_NAME__.client.annotation.Log logAnnotation = method.getAnnotation(__PACKAGE_NAME__.client.annotation.Log.class);
        String moduleName = logAnnotation.module();
        String operation = logAnnotation.operation();

        String requestUri = "";
        String requestMethod = "";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestUri = request.getRequestURI();
            requestMethod = request.getMethod();
        }

        String className = point.getTarget().getClass().getName();
        String methodName = method.getName();
        String args = JSONUtil.toJsonStr(point.getArgs());

        log.info("[{}.{}] {} {} | class={} method={} args={}",
                moduleName, operation, requestMethod, requestUri, className, methodName, args);

        Object result;
        try {
            result = point.proceed();
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[{}.{}] FAILED | cost={}ms | error={}", moduleName, operation, cost, e.getMessage());
            throw e;
        }

        long cost = System.currentTimeMillis() - startTime;
        log.info("[{}.{}] SUCCESS | cost={}ms", moduleName, operation, cost);
        return result;
    }
}
