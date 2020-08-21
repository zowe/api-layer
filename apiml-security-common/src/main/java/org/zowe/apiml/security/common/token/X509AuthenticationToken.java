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
import org.springframework.security.core.GrantedAuthority;

import java.security.cert.X509Certificate;
import java.util.Collection;

public class X509AuthenticationToken extends AbstractAuthenticationToken {

    private X509Certificate[] x509Certificates;

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public X509AuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    public X509AuthenticationToken( X509Certificate[] x509Certificates) {
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
}
