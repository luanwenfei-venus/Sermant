package com.huaweicloud.sermant.core.common;

import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huaweicloud.sermant.core.plugin.classloader.PluginClassLoader;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class AspectForAnnotation {
  @Before("@annotation(com.huaweicloud.sermant.core.plugin.agent.interceptor.EnableUseServiceLoader)")
  public void beforeMethodExecution(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    Object target = joinPoint.getTarget();

    // 仅支持Interceptor的实现类
    if(target instanceof Interceptor){
      if (args.length > 0) {
        ExecuteContext executeContext = (ExecuteContext) args[0];
        PluginClassLoader pluginClassLoader = (PluginClassLoader) target.getClass().getClassLoader();
        pluginClassLoader.setTmpLoader(executeContext.getRawCls().getClassLoader());
      }
    }
  }

  @AfterReturning(pointcut = "@annotation(com.huaweicloud.sermant.core.plugin.agent.interceptor.EnableUseServiceLoader)",
          returning = "result")
  public void afterMethodExecution(JoinPoint joinPoint, Object result) {
    Object[] args = joinPoint.getArgs();
    Object target = joinPoint.getTarget();

    // 仅支持Interceptor的实现类
    if(target instanceof Interceptor){
      if (args.length > 0) {
        PluginClassLoader pluginClassLoader = (PluginClassLoader) target.getClass().getClassLoader();
        pluginClassLoader.removeTmpLoader();
      }
    }
  }
}
