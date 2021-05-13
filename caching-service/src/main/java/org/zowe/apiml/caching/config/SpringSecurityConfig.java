/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${apiml.service.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifyCertificates;

    @Value("${apiml.service.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifyCerts;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @Override
    public void configure(WebSecurity web) throws Exception {
        String[] noSecurityAntMatchers = {
            "/application/health",
            "/application/info",
            "/v2/api-docs"
        };
        web.ignoring().antMatchers(noSecurityAntMatchers);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()   // NOSONAR
            .headers().httpStrictTransportSecurity().disable()
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        if (verifyCertificates || nonStrictVerifyCerts) {
            http.authorizeRequests().anyRequest().authenticated().and()
                .x509().userDetailsService(x509UserDetailsService());
            if (isAttlsEnabled) {
                http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
            }
        } else {
            http.authorizeRequests().anyRequest().permitAll();
        }

    }

    private UserDetailsService x509UserDetailsService() {
        return username -> new User("cachingUser", "", Collections.emptyList());
    }

    static class AttlsFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            X509Certificate[] certificates = new X509Certificate[1];
            String clientCert = request.getHeader("X-SSL-CERT");
            if (clientCert != null) {
                try {
                    clientCert = URLDecoder.decode(clientCert, StandardCharsets.UTF_8.name());
                    InputStream targetStream = new ByteArrayInputStream(clientCert.getBytes());
                    certificates[0] = (X509Certificate) CertificateFactory
                        .getInstance("X509")
                        .generateCertificate(targetStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    filterChain.doFilter(request, response);
                }
                request.setAttribute("javax.servlet.request.X509Certificate", certificates);
            }
            filterChain.doFilter(request, response);
        }

    }
}
