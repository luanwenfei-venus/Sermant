package com.huawei.sermant.premain.spi;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * Agent启动入口
 *
 * @author luanwenfei
 * @since 2023-04-19
 */
public interface AgentEntrance {
    void run(Map<String, Object> argsMap, Instrumentation instrumentation) throws Exception;

    void unInstall();
}
