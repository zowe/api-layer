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

import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.login.x509.X509Authentication;

import java.security.cert.X509Certificate;

@Service
public class X509AuthenticationService implements X509Authentication {

    public String verifyCertificate(X509Certificate certificate) {
        return "user";
    }
}
