/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Builder
@Value
public class RequestCredentials {

    private final String serviceId;
    private final String applId;
    private final Map<String, String> cookies;
    private final Map<String, String[]> headers;
    private final String x509Certificate;

    private final String requestURI;

    static class RequestCredentialsBuilder {

        RequestCredentialsBuilder addCookie(String key, String value) {
            if (this.cookies == null) {
                this.cookies = new HashMap<>();
            }
            this.cookies.put(key, value);

            return this;
        }

        RequestCredentialsBuilder addHeader(String name, String[] value) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.merge(StringUtils.lowerCase(name), value, (a, b) -> ArrayUtils.addAll(a, b));

            return this;
        }

    }

}
