/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509;

import lombok.extern.slf4j.Slf4j;

import java.security.cert.X509Certificate;

@Slf4j
public class X509StaticUserMapper implements X509AuthenticationMapper {
    @Override
    public String mapCertificateToMainframeUserId(X509Certificate certificate) {
        return "apimtst"; // Get username from configuration
    }
}
