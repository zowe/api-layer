/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.logging.annotations;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * When you enable APIML logging with {@link EnableApimlLogger}, you can use EnableApimlLogger as an injector.
 * It injects {@link com.ca.mfaas.message.log.ApimlLogger} instance automatically with
 * {@link com.ca.mfaas.product.logging.ApimlLogInjector} in Spring environment
 */
@Retention(RUNTIME)
@Target(FIELD)
@Documented
public @interface InjectApimlLogger {
}
