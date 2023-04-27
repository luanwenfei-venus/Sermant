/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.core.utils;

import com.huaweicloud.sermant.core.common.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 时间相关工具
 *
 * @author luanwenfei
 * @since 2022-03-19
 */
public class TimeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static long currentTimeMillis;

    private static Object obj = new Object();

    private TimeUtils() {
    }

    /**
     * currentTimeMillis
     *
     * @return long
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
