/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.cache;

/**
 * This interface offer method isExpired. It allow to check state of DTO, because although it is in cache
 * it can be expired.
 *
 * Example:
 *
 *  @CacheEvict(value = &quot;&lt;cacheName&gt;&quot;, condition = &quot;#result != null &amp;&amp; #result.isExpired()&quot;)
 *  @Cacheable(&quot;&lt;cacheName&gt;&quot;)
 *  public Result someBussinessMethod(...)
 *
 */
public interface EntryExpiration {

    /**
     * Method could be use in cache annotation to determinate if record is still valid or expirated.
     *
     * @return true if record is expired, otherwise false
     */
    public boolean isExpired();

}
