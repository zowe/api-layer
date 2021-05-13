/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.filter;

import org.springframework.web.filter.OncePerRequestFilter;
import sun.security.provider.X509Factory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * This filter will add X509 certificate from InboundAttls
 */
public class AttlsFilter extends OncePerRequestFilter {

    private static X509Certificate certificate;

    static {
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(new FileInputStream("./keystore/localhost/localhost.keystore.p12"), "password".toCharArray());
            certificate = (X509Certificate) store.getCertificate("localhost");

        } catch (Exception e) {

        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        X509Certificate[] certificates = new X509Certificate[1];
        String clientCert = request.getHeader("X-SSL-CERT");
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            clientCert = clientCert.replaceAll(X509Factory.BEGIN_CERT, "").replaceAll(X509Factory.END_CERT, "");
            byte [] decoded = Base64.getUrlDecoder().decode(clientCert);
            certificates[0] = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (Exception e) {
            certificates[0] = certificate;
        }
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
        filterChain.doFilter(request, response);
    }

}
