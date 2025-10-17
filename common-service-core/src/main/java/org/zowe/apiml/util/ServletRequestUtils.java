/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class providing helper methods for working with {@link jakarta.servlet.http.HttpServletRequest}.
 */
@Slf4j
public class ServletRequestUtils {

    private static final String CLIENT_CERT_HEADER = "Client-Cert";

    /**
     * Determines whether the client certificate should be ignored based on the Client-Cert HTTP header.
     * @param request the HTTP request to inspect
     * @return true if the client certificate should be ignored, false otherwise.
     */
    public static boolean isClientCertificateIgnored(HttpServletRequest request) {
        var forwardedClientCertificate = request.getHeader(CLIENT_CERT_HEADER);
        if (forwardedClientCertificate == null) {
            // no header means the certificate shouldn't be removed
            log.debug("Request header Client-Cert was not defined.");
            return false;
        }
        // empty header means to ignore the certificate from the request
        return StringUtils.isBlank(forwardedClientCertificate);
    }
}
