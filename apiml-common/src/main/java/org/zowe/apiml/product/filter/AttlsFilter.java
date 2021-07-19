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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.InboundAttls;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This filter will add X509 certificate from InboundAttls
 */
public class AttlsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (InboundAttls.getCertificate() != null && InboundAttls.getCertificate().length > 0) {
                byte[] encodedCert = Base64.encodeBase64(InboundAttls.getCertificate());
                String s = new String(encodedCert);
                s = "-----BEGIN CERTIFICATE-----\n" + s + "\n-----END CERTIFICATE-----";
                X509Certificate certificate = (X509Certificate) CertificateFactory
                    .getInstance("X509")
                    .generateCertificate(new ByteArrayInputStream(s.getBytes()));
                X509Certificate[] certificates = new X509Certificate[1];
                certificates[0] = certificate;
                request.setAttribute("javax.servlet.request.X509Certificate", certificates);
            } else {
                System.out.println("no cert in attls context");
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Not possible to get certificate from context",e);
            response.setStatus(500);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getWriter(), "Exception reading certificate");
        }


    }

}
