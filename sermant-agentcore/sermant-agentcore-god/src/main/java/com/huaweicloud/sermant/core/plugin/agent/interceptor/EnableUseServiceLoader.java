package com.huaweicloud.sermant.core.plugin.agent.interceptor;

/**
 * @author luanwenfei
 * @since 2023-07-19
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnableUseServiceLoader {
}

