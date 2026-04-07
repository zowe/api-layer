/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import java.time.Duration;

public class CacheConstants {

    public static final String INVALIDATED_JWT_TOKENS_CACHE_NAME = "validatedJwtTokens";
    public static final int INVALIDATED_JWT_TOKENS_CACHE_HEAP_SIZE_MB = 1;
    public static final int INVALIDATED_JWT_TOKENS_CACHE_DISK_SIZE_MB = 10;
    public static final Duration INVALIDATED_JWT_TOKENS_CACHE_EXPIRATION = Duration.ofDays(1);



    public static final long SMALL_CACHE_SIZE_ENTRY = 10;
    public static final long BIG_CACHE_SIZE_ENTRY = 1000;

}
