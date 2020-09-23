/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.token;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.security.cert.X509Certificate;
import java.util.Objects;

public class X509AuthenticationToken extends AbstractAuthenticationToken {

    private final X509Certificate[] x509Certificates;

    public X509AuthenticationToken(X509Certificate[] x509Certificates) {
        super(null);
        this.x509Certificates = x509Certificates;
    }

    @Override
    public Object getCredentials() {
        return this.x509Certificates;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }
        return (obj instanceof X509AuthenticationToken) && ((X509AuthenticationToken) obj).getCredentials() == this.getCredentials();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCredentials());
    }
}
