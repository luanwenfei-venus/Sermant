package com.huaweicloud.sermant.core.plugin.agent.enhance;

import com.huaweicloud.sermant.core.classloader.ClassLoaderManager;
import com.huaweicloud.sermant.core.common.CommonConstant;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClassLoader拦截器
 *
 * @author luanwenfei
 * @since 2023-04-28
 */
public class ClassLoaderLoadClassInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        String name = (String)context.getArguments()[0];
        if (ifExclude(name)) {
            LOGGER.info("Load class: " + name + " by sermant.");
            try {
                context.skip(ClassLoaderManager.getPluginClassFinder().loadClassOnlySermant(name, false));
                LOGGER.info("Load class: " + name + " successfully by sermant.");
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, "Can not load class by sermant. And then load by " + context.getObject());
            }
        }
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return null;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) throws Exception {
        return null;
    }

    private boolean ifExclude(String name) {
        for (String excludePrefix : CommonConstant.IGNORE_PREFIXES) {
            if (name.startsWith(excludePrefix)) {
                return true;
            }
        }
        return false;
    }
}
