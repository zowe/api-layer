/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import org.zowe.apiml.gateway.security.login.x509.X509Authentication;

import java.security.cert.X509Certificate;

public class XSSX509Authentication implements X509Authentication {
    @Override
    public String mapUserToCertificate(X509Certificate certificate) {
        return "apimtst"; // Get username from configuration
    }
}
