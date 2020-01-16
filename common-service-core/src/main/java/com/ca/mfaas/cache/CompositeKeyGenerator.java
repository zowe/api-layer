/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.cache;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * Generator for spring cache, which generate {@link CompositeKey}. It allows to
 * get from key its values. It could be helpful for particularly evict of cache.
 */
public class CompositeKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params == null) return CompositeKey.EMPTY;

        switch (params.length) {
            case 0:
                // in case of no parameter, use the same instance of empty key
                return CompositeKey.EMPTY;
            case 1:
                // if there is just one param and it is not array (problem with equals), use just this
                Object param = params[0];
                if (param != null && !param.getClass().isArray()) {
                    return param;
                }
                return new CompositeKey(params);
            default:
                return new CompositeKey(params);
        }
    }

}
