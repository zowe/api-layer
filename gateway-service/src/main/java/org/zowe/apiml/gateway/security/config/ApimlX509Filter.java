/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This filter extends authentication via certificate. It removes all certificates signature from request which are not
 * related to private key using to request signing.
 * <p>
 * Be careful with usage as later on it means that the set of original certificates won't be available.
 */
@RequiredArgsConstructor
public class ApimlX509Filter extends X509AuthenticationFilter {

    private final Set<String> publicKeyCertificatesBase64;

    /**
     * Separate certificates into 2 lists by allowed - see publicKeyCertificatesBase64.
     *  List with key "true" are allowed and "false" not allowed certificates.
     * @param certs all certificated to filter
     * @return map of lists with separated certificates (certs)
     */
    private Map<Boolean, List<X509Certificate>> partitionByAllowed(X509Certificate[] certs) {
        return Arrays.stream(certs).collect(Collectors.partitioningBy(
            cer -> publicKeyCertificatesBase64.contains(
                Base64.getEncoder().encodeToString(cer.getPublicKey().getEncoded())
            )
        ));
    }

    /**
     * Get certificates from request (if exists), separate them (to use only APIML certificate to request sign and
     * other for authentication) and store again into request.
     *
     * @param request Request to filter certificates
     */
    private void filterCerts(ServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null) {
            Map<Boolean, List<X509Certificate>> separatedCerts = partitionByAllowed(certs);
            X509Certificate[] clientAuthCerts = new X509Certificate[separatedCerts.get(false).size()];
            request.setAttribute("client.auth.X509Certificate", separatedCerts.get(false).toArray(clientAuthCerts));
            X509Certificate[] zoweCerts = new X509Certificate[separatedCerts.get(true).size()];
            zoweCerts = separatedCerts.get(true).toArray(zoweCerts);
            request.setAttribute("javax.servlet.request.X509Certificate", zoweCerts);
        }
    }

    /**
     * ApimlX509AuthenticationFilter override methods {@link X509AuthenticationFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}.
     * This filter remove in attribute "javax.servlet.request.X509Certificate" all certificates which has no relations
     * with private certificate using to request sign and then call original implementation (without "foreign"
     * certificates)
     *
     * @param request  request to process
     * @param response response of call
     * @param chain    chain of filters to evaluate
     **/
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        filterCerts(request);
        super.doFilter(request, response, chain);
    }

}
