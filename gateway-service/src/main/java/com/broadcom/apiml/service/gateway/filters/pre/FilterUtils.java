/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.gateway.filters.pre;

public class FilterUtils {

    private FilterUtils() {
        throw new IllegalStateException("Filter utility class");
    }

    public static String removeFirstAndLastSlash(String uri) {
        String normalizedUri = uri;
        if (uri == null) {
            return null;
        }
        if (uri.startsWith("/")) {
            normalizedUri = normalizedUri.substring(1);
        }
        if (uri.endsWith("/")) {
            normalizedUri = normalizedUri.substring(0, normalizedUri.length() - 1);
        }
        return normalizedUri;
    }

    public static String addFirstSlash(String uri) {
        String normalizedUri = uri;
        if (uri == null) {
            return null;
        }
        if (uri.trim().isEmpty()) {
            return "";
        }
        if (!uri.startsWith("/")) {
            normalizedUri = "/" + normalizedUri;
        }
        return normalizedUri;
    }

}
