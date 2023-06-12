/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huaweicloud.sermant.core.plugin.agent.template;

import com.huaweicloud.sermant.core.plugin.agent.adviser.Adviser;
import com.huaweicloud.sermant.core.plugin.agent.entity.ExecuteContext;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.ListIterator;

/**
 * 启动类实例方法advice模板
 * <p>启动类加载器加载类的实例方法如果需要增强，则需要使用该模板
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-10-27
 */
public class BootstrapMemberTemplate {
    private BootstrapMemberTemplate() {
    }

    /**
     * 调用方法的前置触发点
     *
     * @param obj 被增强的对象
     * @param method 被增强的方法
     * @param methodKey 方法键，用于查找模板类
     * @param arguments 方法入参
     * @param adviceClsName advice类名
     * @param context 执行上下文
     * @param isSkip 是否跳过主流程
     * @return 是否跳过主要方法
     * @throws Exception 执行异常
     */
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onMethodEnter(@Advice.This(typing = Assigner.Typing.DYNAMIC) Object obj,
            @Advice.Origin Method method, @Advice.Origin("#t\\##m#s") String methodKey,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] arguments,
            @Advice.Local(value = "_ADVICE_CLASS_NAME_$SERMANT_LOCAL") String adviceClsName,
            @Advice.Local(value = "_EXECUTE_CONTEXT_$SERMANT_LOCAL") Object context,
            @Advice.Local(value = "_IS_SKIP_$SERMANT_LOCAL") Boolean isSkip

    ) throws Throwable {
        adviceClsName = "com.huaweicloud.sermant.core.plugin.agent.template.BootstrapMemberTemplate_"
                + Integer.toHexString(methodKey.hashCode());
        context = ExecuteContext.forMemberMethod(obj, method, arguments, null, null);
        context = Adviser.onMethodEnter(context, adviceClsName);
        arguments = ((ExecuteContext) context).getArguments();
        isSkip = ((ExecuteContext) context).isSkip();
        return isSkip;
    }

    /**
     * 调用方法的后置触发点
     *
     * @param result 方法调用结果
     * @param throwable 方法调用异常
     * @param adviceClsName advice类名
     * @param context 执行上下文
     * @param isSkip 是否跳过主流程
     * @throws Exception 执行异常
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onMethodExit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result,
            @Advice.Thrown Throwable throwable,
            @Advice.Local(value = "_ADVICE_CLASS_NAME_$SERMANT_LOCAL") String adviceClsName,
            @Advice.Local(value = "_EXECUTE_CONTEXT_$SERMANT_LOCAL") Object context,
            @Advice.Local(value = "_IS_SKIP_$SERMANT_LOCAL") Boolean isSkip) throws Throwable {
        context = isSkip ? context : ((ExecuteContext) context).afterMethod(result, throwable);
        context = Adviser.onMethodExit(context, adviceClsName);
        result = ((ExecuteContext) context).getResult();
    }
}