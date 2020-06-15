/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.zowe.apiml.cache.EntryExpiration;

/**
 * This aspect check returned value of a method. It has to return type {@link org.zowe.apiml.cache.EntryExpiration} .
 * If value is expired the method is call again. Method should be annotated by
 * {@see org.zowe.apiml.gateway.cache.RetryIfExpired}.
 *
 * It is helpful in case of caching of entries, which have detection of expiration. With Spring Cache is no easy way to
 * return value from cache and checking expiration in one step. If entry is expired, it could be evicted, but this value
 * is also returned. It can be solved by splitting method (first to evict and second to caching), or with this
 * aspect, like:
 *
 *  @RetryIfExpired
 *  @CacheEvict(value = <cache name>, condition = "#result != null && #result.isExpired()")
 *  @Cacheable(value = <cache name>)
 *  public <? extends EntryExpiration> someMethod(...) { ... }
 *
 * If cache contains expired entry matching to cache key, Cacheable and CacheEvict together remove entry from cache and
 * return it. This aspect then check expiration and call this method once time (in case of expired entry). It will make
 * new fetching of value and caching.
 */
@Aspect
@Component
public class RetryIfExpiredAspect implements Ordered {

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 1;
    }

    @Around("@annotation(org.zowe.apiml.gateway.cache.RetryIfExpired)")
    public Object process(ProceedingJoinPoint pjp) throws Throwable {
        Object returnValue = pjp.proceed(pjp.getArgs());

        if (returnValue == null) return returnValue;
        if (!(returnValue instanceof EntryExpiration)) {
            throw new IllegalArgumentException("Unsupported type : " + returnValue.getClass());
        }

        EntryExpiration entryExpiration = (EntryExpiration) returnValue;
        if (!entryExpiration.isExpired()) return entryExpiration;
        return pjp.proceed(pjp.getArgs());
    }

}
