/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class ApimlKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    /**
     * Was {@code org.apache.http.protocol.HTTP.CONN_KEEP_ALIVE} - the only HttpClient 4 reference in an otherwise
     * HttpClient 5 class, compiling only because Eureka put HttpClient 4 on the classpath. The value is just the
     * header name; HttpClient 5 has no equivalent constant.
     */
    private static final String KEEP_ALIVE_HEADER = "Keep-Alive";

    private static final int KEEPALIVE_TIMOUT_MILLIS = 2000;

    public static final ApimlKeepAliveStrategy INSTANCE = new ApimlKeepAliveStrategy();

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        BasicHeaderElementIterator it = new BasicHeaderElementIterator
            (response.headerIterator(KEEP_ALIVE_HEADER));
        while (it.hasNext()) {
            HeaderElement he = it.next();
            String param = he.getName();
            String value = he.getValue();
            if (value != null && param.equalsIgnoreCase
                ("timeout")) {
                return TimeValue.ofMilliseconds(Long.parseLong(value) * 1000);
            }
        }

        return TimeValue.ofMilliseconds(KEEPALIVE_TIMOUT_MILLIS);
    }
}
