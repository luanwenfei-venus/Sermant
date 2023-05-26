package com.huaweicloud.sermant.core.plugin.agent.enhance;

import com.huaweicloud.sermant.core.classloader.ClassLoaderManager;
import com.huaweicloud.sermant.core.common.CommonConstant;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huaweicloud.sermant.core.plugin.agent.interceptor.Interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 增强findResource方法
 *
 * @author luanwenfei
 * @since 2023-05-08
 */
public class ClassLoaderFindResourceInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        String path = (String)context.getArguments()[0];
        String name = path.replace('/', '.');
        if (ifExclude(name)) {
            LOGGER.info("Find resource: " + path + " by sermant.");
            try {
                context.skip(ClassLoaderManager.getPluginClassLoader().findResource(path));
                LOGGER.info("Find resource: " + name + " successfully by sermant.");
            } catch (Exception exception) {
                LOGGER.log(Level.SEVERE, "Can not find resource by sermant.And then find by " + context.getObject(),
                    exception);
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
