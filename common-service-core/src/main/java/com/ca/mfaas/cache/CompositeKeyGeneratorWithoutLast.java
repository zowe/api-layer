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

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This key generator is for put method. As default it assumes that the last arguments contains value to put.
 */
public class CompositeKeyGeneratorWithoutLast extends CompositeKeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 0) throw new IllegalArgumentException("At least one argument with value is required");
        return super.generate(target, method, Arrays.copyOf(params, params.length - 1));
    }

}
